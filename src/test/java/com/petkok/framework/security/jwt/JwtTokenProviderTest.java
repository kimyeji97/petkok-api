package com.petkok.framework.security.jwt;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * access / refresh 토큰 구분과 만료 검증. 검증 계약 REQ-07-04 ~ 06 (PLAN-REQ-07 § 검증 계약).
 *
 * <p><b>토큰 타입 구분이 이 클래스의 핵심이다.</b> {@code JwtAuthenticationFilter} 가 {@link
 * JwtTokenProvider#isAccessToken(String)} 으로 refresh 토큰의 인증 사용을 막고 있다. 이 검사가 무력해지면 refresh 토큰으로 API
 * 호출이 그대로 뚫린다 — 만료가 긴 토큰이 access 토큰 자리에 서게 된다.
 *
 * <p>DB 없이 도는 순수 로직이다. 계획서가 Testcontainers 를 기각한 근거가 이것이다.
 */
class JwtTokenProviderTest {

  /** HS256 은 256bit 이상을 요구한다 — 32바이트 미만이면 {@code Keys.hmacShaKeyFor} 가 즉시 거부한다. */
  private static final String SECRET = "petkok-test-secret-key-must-be-at-least-32-bytes-long";

  private static JwtTokenProvider provider(long accessValidityMs, long refreshValidityMs) {
    return new JwtTokenProvider(new JwtProperties(SECRET, accessValidityMs, refreshValidityMs));
  }

  @Test
  @DisplayName("[REQ-07-04] refresh 토큰은 access 토큰으로 인정되지 않는다")
  void req_07_04_refreshTokenIsNotAccessToken() {
    String refreshToken = provider(60_000L, 60_000L).createRefreshToken(UUID.randomUUID());

    assertThat(provider(60_000L, 60_000L).isAccessToken(refreshToken)).isFalse();
  }

  @Test
  @DisplayName("[REQ-07-05] access 토큰은 access 토큰으로 인정된다")
  void req_07_05_accessTokenIsAccessToken() {
    String accessToken = provider(60_000L, 60_000L).createAccessToken(UUID.randomUUID());

    assertThat(provider(60_000L, 60_000L).isAccessToken(accessToken)).isTrue();
  }

  /**
   * 만료 시각을 과거로 만들기 위해 <b>음수 유효기간</b>을 주입한다. {@code create()} 가 {@code now + validityMs} 로 만료를 잡으므로
   * 음수면 발급 즉시 만료 상태가 된다 — 테스트에서 {@code sleep} 을 쓰지 않기 위해서다.
   */
  @Test
  @DisplayName("[REQ-07-06] 만료된 토큰은 검증에 실패한다")
  void req_07_06_expiredTokenFailsValidation() {
    String expiredToken = provider(-60_000L, -60_000L).createAccessToken(UUID.randomUUID());

    assertThat(provider(60_000L, 60_000L).validate(expiredToken)).isFalse();
  }
}
