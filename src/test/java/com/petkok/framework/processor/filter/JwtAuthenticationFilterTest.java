package com.petkok.framework.processor.filter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.petkok.framework.security.AuthPrincipal;
import com.petkok.framework.security.UserStatusChecker;
import com.petkok.framework.security.jwt.JwtProperties;
import com.petkok.framework.security.jwt.JwtTokenProvider;
import jakarta.servlet.FilterChain;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 필터의 활성 사용자 검사. 검증 계약 REQ-08-16 ~ 19 (PLAN-REQ-08 § 검증 계약).
 *
 * <p>{@link JwtTokenProvider} 는 <b>실물을 쓴다</b> — 토큰 타입·서명이 필터 분기의 전제라 목으로 바꾸면 검증할 것이 남지 않는다. {@link
 * UserStatusChecker} 만 목이다: 이 테스트의 관심사는 "checker 가 뭐라 답했을 때 필터가 어떻게 하는가"이기 때문이다.
 *
 * <p>⚠️ <b>{@link SecurityContextHolder} 는 ThreadLocal 이라 테스트 간에 샌다.</b> 정리하지 않으면 앞 테스트가 세팅한 인증이 남아
 * 뒤 테스트가 <b>이유 없이 통과</b>한다.
 */
class JwtAuthenticationFilterTest {

  private static final String SECRET = "petkok-test-secret-key-must-be-at-least-32-bytes-long";
  private static final UUID USER_ID = UUID.fromString("11111111-2222-3333-4444-555555555555");

  private final JwtTokenProvider tokenProvider =
      new JwtTokenProvider(new JwtProperties(SECRET, 60_000L, 600_000L));
  private final UserStatusChecker userStatusChecker = mock(UserStatusChecker.class);
  private final JwtAuthenticationFilter filter =
      new JwtAuthenticationFilter(tokenProvider, userStatusChecker);

  private final MockHttpServletResponse response = new MockHttpServletResponse();
  private final FilterChain chain = mock(FilterChain.class);

  @AfterEach
  void clearContext() {
    SecurityContextHolder.clearContext();
  }

  private MockHttpServletRequest withAccessToken() {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader("Authorization", "Bearer " + tokenProvider.createAccessToken(USER_ID));
    return request;
  }

  @Test
  @DisplayName("[REQ-08-16] 탈퇴 사용자 토큰이면 인증이 설정되지 않는다")
  void req_08_16_withdrawnUserIsNotAuthenticated() throws Exception {
    when(userStatusChecker.isActive(USER_ID)).thenReturn(false);

    filter.doFilter(withAccessToken(), response, chain);

    assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
  }

  @Test
  @DisplayName("[REQ-08-16] 탈퇴 사용자여도 필터 체인은 계속된다 (entryPoint 가 401 을 낸다)")
  void req_08_16_withdrawnUserStillProceedsThroughChain() throws Exception {
    when(userStatusChecker.isActive(USER_ID)).thenReturn(false);

    filter.doFilter(withAccessToken(), response, chain);

    verify(chain).doFilter(any(), any());
  }

  @Test
  @DisplayName("[REQ-08-17] 활성 사용자 토큰이면 인증이 설정된다")
  void req_08_17_activeUserIsAuthenticated() throws Exception {
    when(userStatusChecker.isActive(USER_ID)).thenReturn(true);

    filter.doFilter(withAccessToken(), response, chain);

    assertThat(SecurityContextHolder.getContext().getAuthentication().getPrincipal())
        .isEqualTo(new AuthPrincipal(USER_ID));
  }

  @Test
  @DisplayName("[REQ-08-18] 비활성 사용자여도 예외를 던지지 않는다")
  void req_08_18_inactiveUserDoesNotThrow() {
    when(userStatusChecker.isActive(USER_ID)).thenReturn(false);

    assertThatCode(() -> filter.doFilter(withAccessToken(), response, chain))
        .doesNotThrowAnyException();
  }

  @Test
  @DisplayName("[REQ-08-19] 토큰이 없으면 UserStatusChecker 를 호출하지 않는다 (공개 경로에 DB 조회가 안 붙는다)")
  void req_08_19_noTokenSkipsStatusCheck() throws Exception {
    filter.doFilter(new MockHttpServletRequest(), response, chain);

    verify(userStatusChecker, never()).isActive(any());
  }
}
