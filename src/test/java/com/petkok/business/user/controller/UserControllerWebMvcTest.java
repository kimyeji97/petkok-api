package com.petkok.business.user.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.petkok.business.user.service.UserService;
import com.petkok.data.user.dto.UserResponse;
import com.petkok.data.user.dto.UserUpdateRequest;
import com.petkok.framework.config.JacksonConfig;
import com.petkok.framework.config.SecurityConfig;
import com.petkok.framework.security.AuthPrincipal;
import com.petkok.framework.security.jwt.JwtTokenProvider;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/**
 * {@code /users/me} 의 <b>HTTP 계약</b>. 검증 계약 REQ-15-01 ~ 06 (PLAN-REQ-15 § 검증 계약).
 *
 * <p>단위 테스트가 덮지 못하던 지점을 닫는다 — {@code UserServiceTest} 는 서비스 반환값까지, 이 테스트는 <b>직렬화된 JSON 키·상태코드·에러
 * 본문</b>까지 본다. 기존 케이스는 지우지 않는다(D6): {@code REQ-08-01} 은 record 필드 추가를 막고, 여기 {@code REQ-15-01} 은
 * 직렬화를 고정한다. <b>둘 다 깨지면 원인이 바로 갈린다.</b>
 *
 * <p>⚠️ <b>{@code @Import} 를 빼면 이 테스트가 조용히 거짓말한다</b> (PLAN-REQ-15 프로브 결과). 사용자 정의
 * {@code @Configuration} 은 {@code @WebMvcTest} 슬라이스에 <b>포함되지 않는다</b> —
 *
 * <ul>
 *   <li>{@code JacksonConfig} 가 빠지면 응답이 {@code profileImageUrl}(camelCase) 로 나가고, 그걸 단언하면 <b>실제 계약과
 *       반대인 값이 초록불로 굳는다</b>
 *   <li>{@code SecurityConfig} 가 빠지면 Spring Boot 기본 시큐리티가 401 을 내는데 <b>본문이 비어</b> 우리 규격을 검증하지 못한다
 * </ul>
 *
 * <p>⚠️ <b>{@code UserStatusChecker} 를 따로 {@code @MockBean} 하지 말 것.</b> {@link UserService} 가 그
 * 인터페이스를 구현하므로 목끼리 충돌해 {@code UserService} 정의가 사라지고, 에러는 <b>"UserService 없음"으로만 나타나 원인이 안 보인다.</b>
 * {@code @MockBean UserService} 하나가 둘 다 만족시킨다.
 *
 * <p>⚠️ <b>{@code message} 는 단언하지 않는다</b> — 로케일을 탄다(프로브 영어 / 실제 앱 한글). {@code status} 와 {@code
 * error.code} 만 본다.
 */
@WebMvcTest(UserController.class)
@Import({SecurityConfig.class, JacksonConfig.class})
class UserControllerWebMvcTest {

  private static final UUID USER_ID = UUID.fromString("11111111-2222-3333-4444-555555555555");

  @Autowired private MockMvc mockMvc;
  @MockBean private UserService userService;
  @MockBean private JwtTokenProvider jwtTokenProvider;

  /** {@code @WithMockUser} 는 {@link AuthPrincipal} 을 만들지 못해 쓸 수 없다. */
  private static RequestPostProcessor asUser() {
    return authentication(
        new UsernamePasswordAuthenticationToken(
            new AuthPrincipal(USER_ID), null, Collections.emptyList()));
  }

  private void givenProfile() {
    when(userService.getMe(any()))
        .thenReturn(
            new UserResponse(
                USER_ID, "게코집사", null, "https://img.example.com/a.png", LocalDateTime.now()));
  }

  @Test
  @DisplayName("[REQ-15-01] GET /users/me 응답 키가 profile_image_url 이다 (snake_case)")
  void req_15_01_responseUsesSnakeCaseProfileImageUrl() throws Exception {
    givenProfile();

    mockMvc
        .perform(MockMvcRequestBuilders.get("/api/v1/users/me").with(asUser()))
        .andExpect(jsonPath("$.data.profile_image_url").exists());
  }

  @Test
  @DisplayName("[REQ-15-01] GET /users/me 응답 키가 created_at 이다 (snake_case)")
  void req_15_01_responseUsesSnakeCaseCreatedAt() throws Exception {
    givenProfile();

    mockMvc
        .perform(MockMvcRequestBuilders.get("/api/v1/users/me").with(asUser()))
        .andExpect(jsonPath("$.data.created_at").exists());
  }

  @Test
  @DisplayName("[REQ-15-02] GET /users/me 응답에 updated_at 이 없다")
  void req_15_02_responseHasNoUpdatedAt() throws Exception {
    givenProfile();

    mockMvc
        .perform(MockMvcRequestBuilders.get("/api/v1/users/me").with(asUser()))
        .andExpect(jsonPath("$.data.updated_at").doesNotExist());
  }

  @Test
  @DisplayName("[REQ-15-03] PATCH /users/me 101자 닉네임은 400 이다 (500 이 아니다)")
  void req_15_03_nicknameOver100ReturnsBadRequest() throws Exception {
    mockMvc
        .perform(
            MockMvcRequestBuilders.patch("/api/v1/users/me")
                .with(asUser())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nickname\":\"" + "가".repeat(101) + "\"}"))
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("[REQ-15-03] PATCH /users/me 101자 닉네임의 에러 코드는 INVALID_INPUT 이다")
  void req_15_03_nicknameOver100ReturnsInvalidInputCode() throws Exception {
    mockMvc
        .perform(
            MockMvcRequestBuilders.patch("/api/v1/users/me")
                .with(asUser())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nickname\":\"" + "가".repeat(101) + "\"}"))
        .andExpect(jsonPath("$.error.code").value("INVALID_INPUT"));
  }

  @Test
  @DisplayName("[REQ-15-04] DELETE /users/me 는 204 다")
  void req_15_04_withdrawReturnsNoContent() throws Exception {
    mockMvc
        .perform(MockMvcRequestBuilders.delete("/api/v1/users/me").with(asUser()))
        .andExpect(status().isNoContent());
  }

  @Test
  @DisplayName("[REQ-15-04] DELETE /users/me 는 본문이 비어 있다")
  void req_15_04_withdrawHasEmptyBody() throws Exception {
    mockMvc
        .perform(MockMvcRequestBuilders.delete("/api/v1/users/me").with(asUser()))
        .andExpect(content().string(""));
  }

  @Test
  @DisplayName("[REQ-15-05] 탈퇴 사용자 토큰으로 호출하면 401 이다")
  void req_15_05_withdrawnUserGetsUnauthorized() throws Exception {
    when(jwtTokenProvider.validate(any())).thenReturn(true);
    when(jwtTokenProvider.isAccessToken(any())).thenReturn(true);
    when(jwtTokenProvider.getUserId(any())).thenReturn(USER_ID);
    when(userService.isActive(USER_ID)).thenReturn(false);

    mockMvc
        .perform(
            MockMvcRequestBuilders.get("/api/v1/users/me").header("Authorization", "Bearer dummy"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  @DisplayName("[REQ-15-05] 탈퇴 사용자의 에러 코드는 UNAUTHORIZED 다")
  void req_15_05_withdrawnUserGetsUnauthorizedCode() throws Exception {
    when(jwtTokenProvider.validate(any())).thenReturn(true);
    when(jwtTokenProvider.isAccessToken(any())).thenReturn(true);
    when(jwtTokenProvider.getUserId(any())).thenReturn(USER_ID);
    when(userService.isActive(USER_ID)).thenReturn(false);

    mockMvc
        .perform(
            MockMvcRequestBuilders.get("/api/v1/users/me").header("Authorization", "Bearer dummy"))
        .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
  }

  @Test
  @DisplayName("[REQ-15-06] 토큰 없는 요청의 401 본문은 ApiResponse 형태다 (data 가 null)")
  void req_15_06_unauthorizedBodyHasNullData() throws Exception {
    mockMvc
        .perform(MockMvcRequestBuilders.get("/api/v1/users/me"))
        .andExpect(jsonPath("$.data").doesNotExist());
  }

  @Test
  @DisplayName("[REQ-15-06] 토큰 없는 요청의 에러 코드는 UNAUTHORIZED 다")
  void req_15_06_unauthorizedBodyHasErrorCode() throws Exception {
    mockMvc
        .perform(MockMvcRequestBuilders.get("/api/v1/users/me"))
        .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
  }

  // ---- REQ-08 Phase 4 · 프로필 이미지 제거 (D8) ----

  @Test
  @DisplayName("[REQ-08-21] DELETE /users/me/profile-image 는 204 다")
  void req_08_21_removeProfileImageReturnsNoContent() throws Exception {
    mockMvc
        .perform(MockMvcRequestBuilders.delete("/api/v1/users/me/profile-image").with(asUser()))
        .andExpect(status().isNoContent());
  }

  @Test
  @DisplayName("[REQ-08-21] DELETE /users/me/profile-image 는 본문이 비어 있다")
  void req_08_21_removeProfileImageHasEmptyBody() throws Exception {
    mockMvc
        .perform(MockMvcRequestBuilders.delete("/api/v1/users/me/profile-image").with(asUser()))
        .andExpect(content().string(""));
  }

  @Test
  @DisplayName("[REQ-08-25] PATCH 로 profile_image_url: null 을 보내면 서비스에 null 로 전달된다 (변경 없음 의미론 유지)")
  void req_08_25_explicitNullImageReachesServiceAsNull() throws Exception {
    // 병합("null = 변경 없음")은 REQ-08-05 가 서비스에서 고정한다. 여기서는 명시적 null 이
    // 다른 값(예: 빈 문자열)으로 변형되지 않고 그대로 도달하는지를 본다.
    when(userService.updateMe(any(), any()))
        .thenReturn(new UserResponse(USER_ID, "게코집사", null, null, LocalDateTime.now()));

    mockMvc
        .perform(
            MockMvcRequestBuilders.patch("/api/v1/users/me")
                .with(asUser())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"profile_image_url\":null}"))
        .andExpect(status().isOk());

    ArgumentCaptor<UserUpdateRequest> captor = ArgumentCaptor.forClass(UserUpdateRequest.class);
    verify(userService).updateMe(any(), captor.capture());
    assertThat(captor.getValue().profileImageUrl()).isNull();
  }

  // ---- REQ-08 Phase 5 · 닉네임 규칙 (D9) ----

  @Test
  @DisplayName("[REQ-08-26] PATCH 로 nickname: \"\" 을 보내면 400 이다")
  void req_08_26_emptyNicknameIsBadRequest() throws Exception {
    mockMvc
        .perform(
            MockMvcRequestBuilders.patch("/api/v1/users/me")
                .with(asUser())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nickname\":\"\"}"))
        .andExpect(status().isBadRequest());
  }
}
