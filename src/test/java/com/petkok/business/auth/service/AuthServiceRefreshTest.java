package com.petkok.business.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.petkok.business.auth.service.oauth.KakaoOAuthClient;
import com.petkok.data.auth.dto.TokenResponse;
import com.petkok.data.auth.entity.RefreshToken;
import com.petkok.data.auth.repository.RefreshTokenRepository;
import com.petkok.data.user.repository.UserRepository;
import com.petkok.data.user.repository.UserSocialAccountRepository;
import com.petkok.framework.exception.BusinessException;
import com.petkok.framework.exception.ErrorCode;
import com.petkok.framework.security.jwt.JwtProperties;
import com.petkok.framework.security.jwt.JwtTokenProvider;
import com.petkok.framework.util.encrypt.SHA256Util;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.transaction.annotation.Transactional;

/**
 * refresh 로테이션 · 재사용 감지 · 로그아웃. 검증 계약 REQ-07-12 ~ 18 · 21 · 22 (PLAN-REQ-07 § 검증 계약).
 *
 * <p>DB 를 띄우지 않는다. 계획서가 Testcontainers 를 기각한 근거가 이것이다 — 로테이션·재사용 감지는 저장소 호출만 관찰하면 되는 순수 흐름이다.
 * {@link JwtTokenProvider} 와 {@link SHA256Util} 은 <b>실물을 쓴다</b>: 토큰 타입·해시가 계약의 일부라 목으로 바꾸면 검증할 것이
 * 남지 않는다.
 */
class AuthServiceRefreshTest {

  /** HS256 은 256bit 이상을 요구한다 — {@code JwtTokenProviderTest} 와 같은 값. */
  private static final String SECRET = "petkok-test-secret-key-must-be-at-least-32-bytes-long";

  private static final UUID USER_ID = UUID.fromString("11111111-2222-3333-4444-555555555555");

  private final KakaoOAuthClient kakaoOAuthClient = mock(KakaoOAuthClient.class);
  private final UserRepository userRepository = mock(UserRepository.class);
  private final UserSocialAccountRepository socialAccountRepository =
      mock(UserSocialAccountRepository.class);
  private final RefreshTokenRepository refreshTokenRepository = mock(RefreshTokenRepository.class);
  private final JwtTokenProvider jwtTokenProvider =
      new JwtTokenProvider(new JwtProperties(SECRET, 60_000L, 600_000L));

  private final AuthService authService =
      new AuthService(
          kakaoOAuthClient,
          userRepository,
          socialAccountRepository,
          refreshTokenRepository,
          jwtTokenProvider);

  private static String hash(String token) {
    try {
      return SHA256Util.encrypt(token);
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    }
  }

  @Test
  @DisplayName("[REQ-07-12] 재발급하면 제시된 refresh 토큰이 즉시 revoke 된다")
  void req_07_12_presentedTokenIsRevokedOnRotation() {
    String token = jwtTokenProvider.createRefreshToken(USER_ID);
    RefreshToken row = stored(token);

    authService.refresh(token);

    assertThat(row.isRevoked()).isTrue();
  }

  /**
   * ⚠️ <b>같은 초에 발급하면 JWT 문자열이 동일해질 수 있다.</b> {@code iat}/{@code exp} 는 초 단위이고 subject·type 도 같기
   * 때문이다. 그 경우 새 토큰의 해시가 방금 revoke 한 행의 해시와 겹쳐 {@code uq_refresh_tokens_token_hash} 를 위반하거나, 발급하자마자
   * revoke 된 토큰을 클라이언트에 주게 된다. 이 단언은 그 충돌을 잡는 자리이기도 하다.
   */
  @Test
  @DisplayName("[REQ-07-13] 응답의 refresh 토큰은 제시된 것과 다르다")
  void req_07_13_rotatedRefreshTokenDiffers() {
    String token = jwtTokenProvider.createRefreshToken(USER_ID);
    stored(token);

    TokenResponse response = authService.refresh(token);

    assertThat(response.refreshToken()).isNotEqualTo(token);
  }

  @Test
  @DisplayName("[REQ-07-14] 응답의 access 토큰은 access 타입이다")
  void req_07_14_rotatedAccessTokenIsAccessType() {
    String token = jwtTokenProvider.createRefreshToken(USER_ID);
    stored(token);

    TokenResponse response = authService.refresh(token);

    assertThat(jwtTokenProvider.isAccessToken(response.accessToken())).isTrue();
  }

  /** 저장되는 것이 <b>해시</b>임을 원문과 대조해 확인한다 — 원문이 그대로 들어가면 DB 유출 시 재사용이 가능해진다. */
  @Test
  @DisplayName("[REQ-07-15] 새 refresh 토큰은 원문이 아니라 해시로 저장된다")
  void req_07_15_newRefreshTokenIsStoredAsHash() {
    String token = jwtTokenProvider.createRefreshToken(USER_ID);
    stored(token);
    ArgumentCaptor<RefreshToken> saved = ArgumentCaptor.forClass(RefreshToken.class);

    TokenResponse response = authService.refresh(token);
    verify(refreshTokenRepository).save(saved.capture());

    assertThat(saved.getValue().getTokenHash()).isEqualTo(hash(response.refreshToken()));
  }

  @Test
  @DisplayName("[REQ-07-16] revoke 된 토큰이 다시 제시되면 해당 사용자의 토큰을 전부 revoke 한다")
  void req_07_16_reusedTokenRevokesAllForUser() {
    String token = jwtTokenProvider.createRefreshToken(USER_ID);
    stored(token).revoke(LocalDateTime.now());

    catchThrowable(() -> authService.refresh(token));

    verify(refreshTokenRepository).revokeAllByUserId(eq(USER_ID), any(LocalDateTime.class));
  }

  @Test
  @DisplayName("[REQ-07-17] revoke 된 토큰이 다시 제시되면 INVALID_TOKEN 이다")
  void req_07_17_reusedTokenIsInvalidToken() {
    String token = jwtTokenProvider.createRefreshToken(USER_ID);
    stored(token).revoke(LocalDateTime.now());

    assertThatThrownBy(() -> authService.refresh(token))
        .isInstanceOf(BusinessException.class)
        .extracting(thrown -> ((BusinessException) thrown).getErrorCode())
        .isEqualTo(ErrorCode.INVALID_TOKEN);
  }

  @Test
  @DisplayName("[REQ-07-18] 로그아웃은 해당 사용자의 유효 refresh 토큰을 전부 revoke 한다")
  void req_07_18_logoutRevokesAllForUser() {
    authService.logout(USER_ID);

    verify(refreshTokenRepository).revokeAllByUserId(eq(USER_ID), any(LocalDateTime.class));
  }

  /**
   * 만료 판정은 <b>저장된 행의 {@code expires_at}</b> 으로 한다 — 그래서 픽스처도 행의 만료 시각만 과거로 둔다.
   *
   * <p>두 값은 어차피 같다: {@code expires_at} 은 발급 시 토큰의 {@code exp} 를 그대로 읽어 채운다(Phase 4 결정). 만료된 JWT 를
   * 만들어 {@code getExpiresAt} 으로 읽는 방식은 <b>쓸 수 없다</b> — 파싱 단계에서 {@code ExpiredJwtException} 이 먼저
   * 터진다.
   */
  @Test
  @DisplayName("[REQ-07-21] 만료된 refresh 토큰은 INVALID_TOKEN 이다")
  void req_07_21_expiredTokenIsInvalidToken() {
    String token = jwtTokenProvider.createRefreshToken(USER_ID);
    when(refreshTokenRepository.findByTokenHash(hash(token)))
        .thenReturn(
            Optional.of(
                RefreshToken.of(USER_ID, hash(token), LocalDateTime.now().minusMinutes(1))));

    assertThatThrownBy(() -> authService.refresh(token))
        .isInstanceOf(BusinessException.class)
        .extracting(thrown -> ((BusinessException) thrown).getErrorCode())
        .isEqualTo(ErrorCode.INVALID_TOKEN);
  }

  /** 서명이 유효해도 저장소에 행이 없으면 거절한다 — 저장소가 <b>단일 진실</b>이라 로그아웃·탈퇴로 지워진 토큰이 살아나면 안 된다. */
  @Test
  @DisplayName("[REQ-07-22] 저장소에 없는 refresh 토큰은 INVALID_TOKEN 이다")
  void req_07_22_unknownTokenIsInvalidToken() {
    String token = jwtTokenProvider.createRefreshToken(USER_ID);
    when(refreshTokenRepository.findByTokenHash(hash(token))).thenReturn(Optional.empty());

    assertThatThrownBy(() -> authService.refresh(token))
        .isInstanceOf(BusinessException.class)
        .extracting(thrown -> ((BusinessException) thrown).getErrorCode())
        .isEqualTo(ErrorCode.INVALID_TOKEN);
  }

  /**
   * 재사용 감지의 revoke 가 <b>예외와 함께 롤백되지 않아야</b> 한다. 2026-07-30 로컬 왕복에서 실제로 롤백되는 것을 관찰했고 — 401 은 정상적으로
   * 나가므로 <b>겉보기엔 아무 문제가 없었다</b> — 나머지 토큰이 그대로 살아 있었다.
   *
   * <p>저장소를 목으로 대체하는 이 클래스의 다른 케이스로는 <b>원리적으로 잡을 수 없다</b>(목은 롤백되지 않는다). 그래서 동작 대신 애노테이션을 고정한다 — 실제
   * 롤백 여부는 로컬 DB 왕복으로 확인한다.
   */
  @Test
  @DisplayName("[REQ-07-23] refresh 는 BusinessException 에 롤백하지 않는다")
  void req_07_23_refreshDoesNotRollBackOnBusinessException() throws ReflectiveOperationException {
    Transactional tx =
        AuthService.class.getMethod("refresh", String.class).getAnnotation(Transactional.class);

    assertThat(tx.noRollbackFor()).contains(BusinessException.class);
  }

  /** 토큰 원문에 대응하는 행을 저장소에 등록한다. 반환값으로 revoke 여부를 직접 관찰한다. */
  private RefreshToken stored(String token) {
    RefreshToken row = RefreshToken.of(USER_ID, hash(token), jwtTokenProvider.getExpiresAt(token));
    when(refreshTokenRepository.findByTokenHash(hash(token))).thenReturn(Optional.of(row));
    return row;
  }
}
