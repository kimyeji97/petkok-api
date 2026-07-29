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
import java.util.Optional;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 소셜 로그인 · 자동가입 · 토큰 발급.
 *
 * <p>⚠️ <b>{@code data/user} 를 참조한다.</b> 자동가입이 {@code users} 행을 만들기 때문에 피할 수 없다. ArchUnit {@code
 * DomainBoundaryTest} 에 이 참조를 허용하는 예외가 <b>임시로</b> 들어가 있으며, 좁히거나 없애는 방향은 별도 논의 대상이다 (PLAN-REQ-07 「미결
 * 질문」).
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
