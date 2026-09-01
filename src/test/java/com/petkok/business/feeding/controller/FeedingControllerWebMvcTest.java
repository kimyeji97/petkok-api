package com.petkok.business.feeding.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.petkok.business.feeding.service.FeedingService;
import com.petkok.data.feeding.dto.AnorexiaStreakResponse;
import com.petkok.data.feeding.dto.FeedingResponse;
import com.petkok.data.feeding.enums.StreakLevel;
import com.petkok.framework.config.JacksonConfig;
import com.petkok.framework.config.SecurityConfig;
import com.petkok.framework.exception.BusinessException;
import com.petkok.framework.exception.ErrorCode;
import com.petkok.framework.pagination.CursorPage;
import com.petkok.framework.security.AuthPrincipal;
import com.petkok.framework.security.UserStatusChecker;
import com.petkok.framework.security.jwt.JwtTokenProvider;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
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

/**
 * 급여 기록의 <b>HTTP 계약</b>. 검증 계약 REQ-10-43 · 44 · 48 · 52 · 53 · 58 · 66 · 67 (PLAN-REQ-10 § 검증 계약).
 * 구성은 AGENTS §6 관례.
 *
 * <p>⚠️ 이 파일은 {@code FeedingController} 등이 아직 없어 컴파일되지 않는다 — {@code /implement REQ-10 3} 이 만든다.
 */
@WebMvcTest(FeedingController.class)
@Import({SecurityConfig.class, JacksonConfig.class})
class FeedingControllerWebMvcTest {

  private static final UUID USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
  private static final UUID PET_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
  private static final UUID LOG_ID = UUID.fromString("cccccccc-0000-0000-0000-000000000001");
  private static final String BASE = "/api/v1/pets/" + PET_ID + "/feeding";

  @Autowired private MockMvc mockMvc;
  @MockBean private FeedingService feedingService;
  @MockBean private JwtTokenProvider jwtTokenProvider;
  @MockBean private UserStatusChecker userStatusChecker;

  private static RequestPostProcessor asUser() {
    return authentication(
        new UsernamePasswordAuthenticationToken(
            new AuthPrincipal(USER_ID), null, Collections.emptyList()));
  }

  private static String body(String extraFields) {
    return "{\"is_refused\":false,\"fed_at\":\"2026-06-30T09:00:00Z\"" + extraFields + "}";
  }

  private static FeedingResponse sample() {
    return new FeedingResponse(
        LOG_ID,
        PET_ID,
        "귀뚜라미",
        null,
        null,
        null,
        false,
        OffsetDateTime.now(),
        null,
        OffsetDateTime.now());
  }

  @Test
  @DisplayName("[REQ-10-43] POST /feeding 은 201 을 반환한다")
  void req_10_43_createReturnsCreated() throws Exception {
    when(feedingService.create(any(), any(), any())).thenReturn(sample());

    mockMvc
        .perform(
            MockMvcRequestBuilders.post(BASE)
                .with(asUser())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body("")))
        .andExpect(status().isCreated());
  }

  @Test
  @DisplayName("[REQ-10-44] DELETE /feeding/{log_id} 는 204 다")
  void req_10_44_deleteReturnsNoContent() throws Exception {
    mockMvc
        .perform(MockMvcRequestBuilders.delete(BASE + "/" + LOG_ID).with(asUser()))
        .andExpect(status().isNoContent());
  }

  @Test
  @DisplayName("[REQ-10-44] DELETE /feeding/{log_id} 는 본문이 비어 있다")
  void req_10_44_deleteHasEmptyBody() throws Exception {
    mockMvc
        .perform(MockMvcRequestBuilders.delete(BASE + "/" + LOG_ID).with(asUser()))
        .andExpect(content().string(""));
  }

  @Test
  @DisplayName("[REQ-10-48] GET /feeding 응답에 items · next_cursor · has_next 키가 있다")
  void req_10_48_listResponseHasCursorPageKeys() throws Exception {
    when(feedingService.list(any(), any(), any()))
        .thenReturn(CursorPage.of(List.of(sample()), null, false));

    mockMvc
        .perform(MockMvcRequestBuilders.get(BASE).with(asUser()))
        .andExpect(jsonPath("$.data.items").isArray())
        .andExpect(jsonPath("$.data.next_cursor").hasJsonPath())
        .andExpect(jsonPath("$.data.has_next").value(false));
  }

  @Test
  @DisplayName("[REQ-10-52] is_refused 가 없으면 400 이다")
  void req_10_52_missingIsRefusedIsRejected() throws Exception {
    mockMvc
        .perform(
            MockMvcRequestBuilders.post(BASE)
                .with(asUser())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"fed_at\":\"2026-06-30T09:00:00Z\"}"))
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("[REQ-10-53] fed_at 이 없으면 400 이다")
  void req_10_53_missingFedAtIsRejected() throws Exception {
    mockMvc
        .perform(
            MockMvcRequestBuilders.post(BASE)
                .with(asUser())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"is_refused\":false}"))
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("[REQ-10-58] food_size 가 정의되지 않은 값이면 400 이다")
  void req_10_58_unknownFoodSizeIsRejected() throws Exception {
    mockMvc
        .perform(
            MockMvcRequestBuilders.post(BASE)
                .with(asUser())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body(",\"food_size\":\"XL\"")))
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("[REQ-10-66] 개 펫의 거식 스트릭 조회는 HTTP 왕복에서 400 이다")
  void req_10_66_nonGeckoStreakIsBadRequestStatus() throws Exception {
    when(feedingService.getAnorexiaStreak(any(), any()))
        .thenThrow(new BusinessException(ErrorCode.FEATURE_NOT_SUPPORTED_SPECIES));

    mockMvc
        .perform(MockMvcRequestBuilders.get(BASE + "/anorexia-streak").with(asUser()))
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("[REQ-10-66] 개 펫의 거식 스트릭 조회는 error.code 가 FEATURE_NOT_SUPPORTED_SPECIES 다")
  void req_10_66_nonGeckoStreakHasErrorCode() throws Exception {
    when(feedingService.getAnorexiaStreak(any(), any()))
        .thenThrow(new BusinessException(ErrorCode.FEATURE_NOT_SUPPORTED_SPECIES));

    mockMvc
        .perform(MockMvcRequestBuilders.get(BASE + "/anorexia-streak").with(asUser()))
        .andExpect(jsonPath("$.error.code").value("FEATURE_NOT_SUPPORTED_SPECIES"));
  }

  @Test
  @DisplayName("[REQ-10-67] 게코 펫의 거식 스트릭 조회는 200 이다")
  void req_10_67_geckoStreakReturnsOk() throws Exception {
    when(feedingService.getAnorexiaStreak(any(), any()))
        .thenReturn(new AnorexiaStreakResponse(0, StreakLevel.NONE, null));

    mockMvc
        .perform(MockMvcRequestBuilders.get(BASE + "/anorexia-streak").with(asUser()))
        .andExpect(status().isOk());
  }
}
