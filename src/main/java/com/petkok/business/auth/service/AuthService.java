package com.petkok.business.auth.service;

import com.petkok.business.auth.service.oauth.KakaoOAuthClient;
import com.petkok.data.auth.dto.KakaoTokenResponse;
import com.petkok.data.auth.dto.OAuthProfileResponse;
import com.petkok.data.auth.dto.TokenResponse;
import com.petkok.data.auth.entity.RefreshToken;
import com.petkok.data.auth.repository.RefreshTokenRepository;
import com.petkok.data.user.entity.User;
import com.petkok.data.user.entity.UserSocialAccount;
import com.petkok.data.user.enums.SocialProvider;
import com.petkok.data.user.repository.UserRepository;
import com.petkok.data.user.repository.UserSocialAccountRepository;
import com.petkok.framework.exception.BusinessException;
import com.petkok.framework.exception.ErrorCode;
import com.petkok.framework.security.jwt.JwtTokenProvider;
import com.petkok.framework.util.encrypt.SHA256Util;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 소셜 로그인 · 자동가입 · 토큰 발급.
 *
 * <p><b>{@code data/user} 를 참조한다 — 설계상 옳다.</b> 소셜 자동가입은 본질적으로 user 프로비저닝이므로 {@code users} 행을 만드는 것이
 * 이 서비스의 제 일이다. ArchUnit {@code DomainBoundaryTest} 의 예외도 2026-07-31 에 <b>임시에서 설계 결정으로 승격</b>됐다
 * (PLAN-REQ-08 D4). 없애려면 {@code business/user} 에 프로비저닝 진입점을 두어야 하는데, 그러면 {@code business/auth →
 * business/user} 예외가 대신 생겨 <b>개수는 그대로인 채 간접층만 는다.</b>
 */
@Slf4j
@Service
public class AuthService {

  private final KakaoOAuthClient kakaoOAuthClient;
  private final UserRepository userRepository;
  private final UserSocialAccountRepository socialAccountRepository;
  private final RefreshTokenRepository refreshTokenRepository;
  private final JwtTokenProvider jwtTokenProvider;

  public AuthService(
      KakaoOAuthClient kakaoOAuthClient,
      UserRepository userRepository,
      UserSocialAccountRepository socialAccountRepository,
      RefreshTokenRepository refreshTokenRepository,
      JwtTokenProvider jwtTokenProvider) {
    this.kakaoOAuthClient = kakaoOAuthClient;
    this.userRepository = userRepository;
    this.socialAccountRepository = socialAccountRepository;
    this.refreshTokenRepository = refreshTokenRepository;
    this.jwtTokenProvider = jwtTokenProvider;
  }

  /**
   * 카카오 인가코드로 로그인한다. 연결된 계정이 없으면 자동가입한다.
   *
   * <p>⚠️ <b>인가 코드는 1회용이고 약 10분 만료다.</b> 토큰 교환이 성공한 뒤 이 메서드가 실패하면 클라이언트는 <b>같은 코드로 재시도할 수 없고</b> 다시
   * 로그인해야 한다.
   *
   * @param authorizationCode 클라이언트가 카카오에서 받아 넘긴 인가코드
   */
  @Transactional
  public TokenResponse loginWithKakao(String authorizationCode) {
    KakaoTokenResponse kakaoToken = kakaoOAuthClient.exchangeToken(authorizationCode);
    OAuthProfileResponse profile = kakaoOAuthClient.getProfile(kakaoToken.accessToken());

    User user = findOrCreateUser(SocialProvider.KAKAO, profile);
    return issueTokens(user.getId());
  }

  /**
   * {@code (provider, provider_user_id)} 로 찾고 없으면 만든다.
   *
   * <p>이 조합이 <b>유일한 식별자</b>다 — 카카오는 이메일을 주지 않으므로(2026-07-29 실측) 이메일 기반 계정 병합은 성립하지 않는다. 같은 사람이 다른
   * provider 로 가입하면 별개 계정이 된다.
   */
  private User findOrCreateUser(SocialProvider provider, OAuthProfileResponse profile) {
    Optional<UserSocialAccount> linked =
        socialAccountRepository.findByProviderAndProviderUserId(provider, profile.providerUserId());
    if (linked.isPresent()) {
      return linked.get().getUser();
    }

    User user =
        userRepository.save(
            User.of(profile.nickname(), profile.email(), profile.profileImageUrl()));
    try {
      socialAccountRepository.saveAndFlush(
          UserSocialAccount.of(user, provider, profile.providerUserId()));
    } catch (DataIntegrityViolationException e) {
      // UNIQUE(provider, provider_user_id) 위반 = 동시 요청으로 같은 계정이 먼저 만들어졌다.
      // 조회 시점엔 없었는데 저장 시점엔 있는 경우라, 여기서 삼키면 중복 계정이 생긴다.
      log.warn("Social account already linked. provider={}", provider, e);
      throw new BusinessException(ErrorCode.SOCIAL_ALREADY_LINKED);
    }
    return user;
  }

  /**
   * refresh 토큰 로테이션. 제시된 토큰을 즉시 revoke 하고 access + refresh 를 새로 발급한다. 검증 계약 REQ-07-12~17 · 21 · 22.
   *
   * <p><b>제시된 토큰이 이미 revoke 돼 있으면 탈취로 간주한다.</b> 정상 클라이언트는 로테이션 직후 새 토큰으로 교체하므로 revoke 된 토큰을 다시 보낼
   * 일이 없다. 이때는 해당 사용자의 <b>모든</b> refresh 를 무효화한다 — 공격자와 사용자 중 누가 쥔 토큰인지 구분할 수 없으므로 둘 다 끊고 재로그인시키는
   * 쪽이 안전하다.
   *
   * <p>⚠️ <b>{@code noRollbackFor} 가 재사용 감지의 핵심이다.</b> 기본 설정이면 아래 {@code revokeAllByUserId} 가 실행돼도
   * 뒤이어 던지는 예외가 트랜잭션을 통째로 롤백해 <b>무효화가 없던 일이 된다</b> — 401 은 그대로 나가므로 <b>겉보기엔 정상 동작</b>이고, 공격자가 쥔 나머지
   * 토큰만 조용히 살아남는다. 2026-07-30 로컬 왕복에서 실제로 관찰했다(목 기반 단위 테스트로는 잡히지 않는다). 제거 방지용으로 REQ-07-23 이 이
   * 애노테이션을 고정한다.
   *
   * <p>⚠️ <b>동시 요청은 아직 구분하지 못한다.</b> 앱 재시작·병렬 요청으로 같은 토큰이 거의 동시에 두 번 오면 정상 사용자도 전 기기 로그아웃된다 — 유예
   * 윈도우 도입 여부는 PLAN-REQ-07 「미결 질문」에 열려 있다.
   *
   * @param refreshToken 클라이언트가 body 로 제시한 refresh 토큰 <b>원문</b>
   */
  @Transactional(noRollbackFor = BusinessException.class)
  public TokenResponse refresh(String refreshToken) {
    RefreshToken stored =
        refreshTokenRepository
            .findByTokenHash(hash(refreshToken))
            .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_TOKEN));

    LocalDateTime now = LocalDateTime.now();
    if (stored.isRevoked()) {
      // 재사용 감지. 여기서 조용히 거절만 하면 공격자가 쥔 다른 토큰은 계속 살아 있다.
      log.warn("Revoked refresh token reused. userId={}", stored.getUserId());
      refreshTokenRepository.revokeAllByUserId(stored.getUserId(), now);
      throw new BusinessException(ErrorCode.INVALID_TOKEN);
    }
    if (stored.isExpired(now)) {
      throw new BusinessException(ErrorCode.INVALID_TOKEN);
    }

    stored.revoke(now);
    return issueTokens(stored.getUserId());
  }

  /**
   * 로그아웃. 해당 사용자의 유효 refresh 를 <b>전부</b> revoke 한다. 검증 계약 REQ-07-18.
   *
   * <p>Request Body 가 없어 <b>access 토큰이 유일한 식별 수단</b>이다(api-list § 1. Auth) — 어떤 기기의 토큰인지 지목할 방법이
   * 없으므로 기기별 로그아웃은 성립하지 않는다. api-list § refresh 토큰 저장소의 "해당 토큰 revoked_at" 서술과 어긋나며, 원본 확인 건으로
   * PLAN-REQ-07 「미결 질문」에 올려 뒀다.
   *
   * @param userId access 토큰에서 꺼낸 사용자 식별자
   */
  @Transactional
  public void logout(UUID userId) {
    refreshTokenRepository.revokeAllByUserId(userId, LocalDateTime.now());
  }

  /**
   * access + refresh 를 발급하고 refresh <b>해시</b>를 저장한다.
   *
   * <p>토큰 원문은 저장하지 않는다 — DB 가 유출되면 그대로 재사용 가능해지기 때문이다.
   */
  private TokenResponse issueTokens(UUID userId) {
    String accessToken = jwtTokenProvider.createAccessToken(userId);
    String refreshToken = jwtTokenProvider.createRefreshToken(userId);

    refreshTokenRepository.save(
        RefreshToken.of(userId, hash(refreshToken), jwtTokenProvider.getExpiresAt(refreshToken)));

    return new TokenResponse(accessToken, refreshToken);
  }

  /** SHA-256 hex(64자). 알고리즘은 JDK 표준이라 실패는 환경 이상이며 복구 대상이 아니다. */
  private static String hash(String token) {
    try {
      return SHA256Util.encrypt(token);
    } catch (NoSuchAlgorithmException e) {
      throw new BusinessException(ErrorCode.INTERNAL_ERROR, e.getMessage());
    }
  }
}
