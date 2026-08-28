package com.petkok.business.activity.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.petkok.business.activity.service.ActivityService;
import com.petkok.data.activity.dto.ActivityResponse;
import com.petkok.data.activity.enums.ActivityType;
import com.petkok.framework.config.JacksonConfig;
import com.petkok.framework.config.SecurityConfig;
import com.petkok.framework.exception.BusinessException;
import com.petkok.framework.exception.ErrorCode;
import com.petkok.framework.security.AuthPrincipal;
import com.petkok.framework.security.UserStatusChecker;
import com.petkok.framework.security.jwt.JwtTokenProvider;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/** 활동 기록의 <b>HTTP 계약</b>. 검증 계약 REQ-10-32 ~ 37 (PLAN-REQ-10 § 검증 계약). 구성은 AGENTS §6 관례. */
@WebMvcTest(ActivityController.class)
@Import({SecurityConfig.class, JacksonConfig.class})
class ActivityControllerWebMvcTest {

  private static final UUID USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
  private static final UUID PET_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
  private static final UUID LOG_ID = UUID.fromString("bbbbbbbb-0000-0000-0000-000000000001");
  private static final String BASE = "/api/v1/pets/" + PET_ID + "/activity";

  @Autowired private MockMvc mockMvc;
  @MockBean private ActivityService activityService;
  @MockBean private JwtTokenProvider jwtTokenProvider;
  @MockBean private UserStatusChecker userStatusChecker;

  private static RequestPostProcessor asUser() {
    return authentication(
        new UsernamePasswordAuthenticationToken(
            new AuthPrincipal(USER_ID), null, Collections.emptyList()));
  }

  private static String body(String type) {
    return "{\"activity_type\":\"" + type + "\",\"logged_at\":\"2026-06-30T09:00:00Z\"}";
  }

  @Test
  @DisplayName("[REQ-10-32] POST /activity 는 201 을 반환한다")
  void req_10_32_createReturnsCreated() throws Exception {
    when(activityService.create(any(), any(), any()))
        .thenReturn(
            new ActivityResponse(
                LOG_ID,
                PET_ID,
                ActivityType.WALK,
                30,
                null,
                null,
                OffsetDateTime.now(),
                OffsetDateTime.now()));

    mockMvc
        .perform(
            MockMvcRequestBuilders.post(BASE)
                .with(asUser())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body("WALK")))
        .andExpect(status().isCreated());
  }

  @Test
  @DisplayName("[REQ-10-33] DELETE /activity/{log_id} 는 204 다")
  void req_10_33_deleteReturnsNoContent() throws Exception {
    mockMvc
        .perform(MockMvcRequestBuilders.delete(BASE + "/" + LOG_ID).with(asUser()))
        .andExpect(status().isNoContent());
  }

  @Test
  @DisplayName("[REQ-10-33] DELETE /activity/{log_id} 는 본문이 비어 있다")
  void req_10_33_deleteHasEmptyBody() throws Exception {
    mockMvc
        .perform(MockMvcRequestBuilders.delete(BASE + "/" + LOG_ID).with(asUser()))
        .andExpect(content().string(""));
  }

  @Test
  @DisplayName("[REQ-10-34] 종 위반은 HTTP 왕복에서 400 이다")
  void req_10_34_speciesViolationIsBadRequestStatus() throws Exception {
    when(activityService.create(any(), any(), any()))
        .thenThrow(new BusinessException(ErrorCode.INVALID_SPECIES_ACTIVITY));

    mockMvc
        .perform(
            MockMvcRequestBuilders.post(BASE)
                .with(asUser())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body("WALK")))
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("[REQ-10-34] 종 위반은 HTTP 왕복에서 error.code 가 INVALID_SPECIES_ACTIVITY 다")
  void req_10_34_speciesViolationHasErrorCode() throws Exception {
    when(activityService.create(any(), any(), any()))
        .thenThrow(new BusinessException(ErrorCode.INVALID_SPECIES_ACTIVITY));

    mockMvc
        .perform(
            MockMvcRequestBuilders.post(BASE)
                .with(asUser())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body("WALK")))
        .andExpect(jsonPath("$.error.code").value("INVALID_SPECIES_ACTIVITY"));
  }

  @Test
  @DisplayName("[REQ-10-35] 정의되지 않은 activity_type 값은 400 이다")
  void req_10_35_unknownTypeIsRejected() throws Exception {
    mockMvc
        .perform(
            MockMvcRequestBuilders.post(BASE)
                .with(asUser())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body("SWIM")))
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("[REQ-10-36] activity_type 이 없으면 400 이다")
  void req_10_36_missingTypeIsRejected() throws Exception {
    mockMvc
        .perform(
            MockMvcRequestBuilders.post(BASE)
                .with(asUser())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"logged_at\":\"2026-06-30T09:00:00Z\"}"))
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("[REQ-10-37] logged_at 이 없으면 400 이다")
  void req_10_37_missingLoggedAtIsRejected() throws Exception {
    mockMvc
        .perform(
            MockMvcRequestBuilders.post(BASE)
                .with(asUser())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"activity_type\":\"WALK\"}"))
        .andExpect(status().isBadRequest());
  }
}
