package com.petkok.business.auth.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.petkok.business.auth.service.AuthService;
import com.petkok.framework.config.JacksonConfig;
import com.petkok.framework.config.SecurityConfig;
import com.petkok.framework.security.AuthPrincipal;
import com.petkok.framework.security.UserStatusChecker;
import com.petkok.framework.security.jwt.JwtTokenProvider;
import java.util.Collections;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/**
 * {@code /auth} 의 <b>HTTP 계약</b>. 검증 계약 REQ-15-07 · 08 (PLAN-REQ-15 § 검증 계약).
 *
 * <p><b>여기에 auth 를 포함한 이유는 관례 검증이다</b> (D8). {@code /auth/kakao} 는 공개 경로, {@code /auth/logout} 은 인증
 * 경로라 <b>한 설정이 양쪽을 버티는지</b> 이 클래스에서 확인된다.
 *
 * <p>여기서는 {@link UserStatusChecker} 를 <b>따로 목으로 둬야 한다</b> — {@code UserControllerWebMvcTest} 와 달리 이
 * 슬라이스에는 {@code UserService} 가 없어 포트를 채울 것이 없기 때문이다. 필터는 슬라이스에 자동 포함되므로 ({@code Filter} 빈) 비워 두면
 * 컨텍스트가 뜨지 않는다.
 */
@WebMvcTest(AuthController.class)
@Import({SecurityConfig.class, JacksonConfig.class})
class AuthControllerWebMvcTest {

  private static final UUID USER_ID = UUID.fromString("11111111-2222-3333-4444-555555555555");

  @Autowired private MockMvc mockMvc;
  @MockBean private AuthService authService;
  @MockBean private JwtTokenProvider jwtTokenProvider;
  @MockBean private UserStatusChecker userStatusChecker;

  private static RequestPostProcessor asUser() {
    return authentication(
        new UsernamePasswordAuthenticationToken(
            new AuthPrincipal(USER_ID), null, Collections.emptyList()));
  }

  @Test
  @DisplayName("[REQ-15-07] POST /auth/kakao 는 무인증 접근이 401 이 아니다 (공개 경로)")
  void req_15_07_kakaoLoginIsPublicPath() throws Exception {
    // 상태코드를 정확히 단언하지 않는다. 이 케이스가 재는 것은 "시큐리티에 막히지 않는다"이지
    // 검증 동작이 아니다 — 빈 body 는 code 의 @NotBlank 때문에 400 이 된다.
    int status =
        mockMvc
            .perform(
                MockMvcRequestBuilders.post("/api/v1/auth/kakao")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{}"))
            .andReturn()
            .getResponse()
            .getStatus();

    assertThat(status).isNotEqualTo(HttpStatus.UNAUTHORIZED.value());
  }

  @Test
  @DisplayName("[REQ-15-08] DELETE /auth/logout 은 204 다")
  void req_15_08_logoutReturnsNoContent() throws Exception {
    mockMvc
        .perform(MockMvcRequestBuilders.delete("/api/v1/auth/logout").with(asUser()))
        .andExpect(status().isNoContent());
  }

  @Test
  @DisplayName("[REQ-15-08] DELETE /auth/logout 은 본문이 비어 있다")
  void req_15_08_logoutHasEmptyBody() throws Exception {
    mockMvc
        .perform(MockMvcRequestBuilders.delete("/api/v1/auth/logout").with(asUser()))
        .andExpect(content().string(""));
  }
}
