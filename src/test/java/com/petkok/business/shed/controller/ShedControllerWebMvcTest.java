package com.petkok.business.shed.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.petkok.business.shed.service.ShedService;
import com.petkok.data.shed.dto.ShedPredictionResponse;
import com.petkok.data.shed.dto.ShedResponse;
import com.petkok.data.shed.enums.PredictionConfidence;
import com.petkok.framework.config.JacksonConfig;
import com.petkok.framework.config.SecurityConfig;
import com.petkok.framework.exception.BusinessException;
import com.petkok.framework.exception.ErrorCode;
import com.petkok.framework.pagination.CursorPage;
import com.petkok.framework.security.AuthPrincipal;
import com.petkok.framework.security.UserStatusChecker;
import com.petkok.framework.security.jwt.JwtTokenProvider;
import java.time.LocalDate;
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
 * 탈피 기록의 <b>HTTP 계약</b>. 검증 계약 REQ-10-68 · 69 · 73 · 77 · 79 ~ 82 · 92 · 93 (PLAN-REQ-10 § 검증 계약).
 * 구성은 AGENTS §6 관례.
 *
 * <p>⚠️ 이 파일은 {@code ShedController} 등이 아직 없어 컴파일되지 않는다 — {@code /implement REQ-10 4} 가 만든다.
 */
@WebMvcTest(ShedController.class)
@Import({SecurityConfig.class, JacksonConfig.class})
class ShedControllerWebMvcTest {

  private static final UUID USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
  private static final UUID PET_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
  private static final UUID RECORD_ID = UUID.fromString("dddddddd-0000-0000-0000-000000000001");
  private static final String BASE = "/api/v1/pets/" + PET_ID + "/shed";

  @Autowired private MockMvc mockMvc;
  @MockBean private ShedService shedService;
  @MockBean private JwtTokenProvider jwtTokenProvider;
  @MockBean private UserStatusChecker userStatusChecker;

  private static RequestPostProcessor asUser() {
    return authentication(
        new UsernamePasswordAuthenticationToken(
            new AuthPrincipal(USER_ID), null, Collections.emptyList()));
  }

  private static ShedResponse sample() {
    return new ShedResponse(
        RECORD_ID, PET_ID, LocalDate.of(2026, 7, 20), true, false, null, OffsetDateTime.now());
  }

  private void mockSpeciesRejection() {
    when(shedService.create(any(), any(), any()))
        .thenThrow(new BusinessException(ErrorCode.FEATURE_NOT_SUPPORTED_SPECIES));
    when(shedService.list(any(), any(), any()))
        .thenThrow(new BusinessException(ErrorCode.FEATURE_NOT_SUPPORTED_SPECIES));
    when(shedService.update(any(), any(), any(), any()))
        .thenThrow(new BusinessException(ErrorCode.FEATURE_NOT_SUPPORTED_SPECIES));
    org.mockito.Mockito.doThrow(new BusinessException(ErrorCode.FEATURE_NOT_SUPPORTED_SPECIES))
        .when(shedService)
        .delete(any(), any(), any());
  }

  @Test
  @DisplayName("[REQ-10-68] POST /shed 는 201 을 반환한다")
  void req_10_68_createReturnsCreated() throws Exception {
    when(shedService.create(any(), any(), any())).thenReturn(sample());

    mockMvc
        .perform(
            MockMvcRequestBuilders.post(BASE)
                .with(asUser())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"shed_date\":\"2026-07-20\"}"))
        .andExpect(status().isCreated());
  }

  @Test
  @DisplayName("[REQ-10-69] DELETE /shed/{record_id} 는 204 다")
  void req_10_69_deleteReturnsNoContent() throws Exception {
    mockMvc
        .perform(MockMvcRequestBuilders.delete(BASE + "/" + RECORD_ID).with(asUser()))
        .andExpect(status().isNoContent());
  }

  @Test
  @DisplayName("[REQ-10-69] DELETE /shed/{record_id} 는 본문이 비어 있다")
  void req_10_69_deleteHasEmptyBody() throws Exception {
    mockMvc
        .perform(MockMvcRequestBuilders.delete(BASE + "/" + RECORD_ID).with(asUser()))
        .andExpect(content().string(""));
  }

  @Test
  @DisplayName("[REQ-10-73] GET /shed 응답에 items · next_cursor · has_next 키가 있다")
  void req_10_73_listResponseHasCursorPageKeys() throws Exception {
    when(shedService.list(any(), any(), any()))
        .thenReturn(CursorPage.of(List.of(sample()), null, false));

    mockMvc
        .perform(MockMvcRequestBuilders.get(BASE).with(asUser()))
        .andExpect(jsonPath("$.data.items").isArray())
        .andExpect(jsonPath("$.data.next_cursor").hasJsonPath())
        .andExpect(jsonPath("$.data.has_next").value(false));
  }

  @Test
  @DisplayName("[REQ-10-77] shed_date 가 없으면 400 이다")
  void req_10_77_missingShedDateIsRejected() throws Exception {
    mockMvc
        .perform(
            MockMvcRequestBuilders.post(BASE)
                .with(asUser())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("[REQ-10-79] 개 펫의 POST /shed 는 HTTP 왕복에서 400 이다")
  void req_10_79_dogCreateIsBadRequestStatus() throws Exception {
    mockSpeciesRejection();

    mockMvc
        .perform(
            MockMvcRequestBuilders.post(BASE)
                .with(asUser())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"shed_date\":\"2026-07-20\"}"))
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("[REQ-10-79] 개 펫의 POST /shed 는 error.code 가 FEATURE_NOT_SUPPORTED_SPECIES 다")
  void req_10_79_dogCreateHasErrorCode() throws Exception {
    mockSpeciesRejection();

    mockMvc
        .perform(
            MockMvcRequestBuilders.post(BASE)
                .with(asUser())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"shed_date\":\"2026-07-20\"}"))
        .andExpect(jsonPath("$.error.code").value("FEATURE_NOT_SUPPORTED_SPECIES"));
  }

  @Test
  @DisplayName("[REQ-10-80] 개 펫의 GET /shed 는 400 이다 (목록도 게코 전용)")
  void req_10_80_dogListIsBadRequest() throws Exception {
    mockSpeciesRejection();

    mockMvc
        .perform(MockMvcRequestBuilders.get(BASE).with(asUser()))
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("[REQ-10-81] 개 펫의 PATCH /shed/{record_id} 는 400 이다")
  void req_10_81_dogUpdateIsBadRequest() throws Exception {
    mockSpeciesRejection();

    mockMvc
        .perform(
            MockMvcRequestBuilders.patch(BASE + "/" + RECORD_ID)
                .with(asUser())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("[REQ-10-82] 개 펫의 DELETE /shed/{record_id} 는 400 이다")
  void req_10_82_dogDeleteIsBadRequest() throws Exception {
    mockSpeciesRejection();

    mockMvc
        .perform(MockMvcRequestBuilders.delete(BASE + "/" + RECORD_ID).with(asUser()))
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("[REQ-10-92] 개 펫의 탈피 예측 조회는 HTTP 왕복에서 400 이다")
  void req_10_92_dogPredictionIsBadRequestStatus() throws Exception {
    when(shedService.getPrediction(any(), any()))
        .thenThrow(new BusinessException(ErrorCode.FEATURE_NOT_SUPPORTED_SPECIES));

    mockMvc
        .perform(MockMvcRequestBuilders.get(BASE + "/prediction").with(asUser()))
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("[REQ-10-92] 개 펫의 탈피 예측 조회는 error.code 가 FEATURE_NOT_SUPPORTED_SPECIES 다")
  void req_10_92_dogPredictionHasErrorCode() throws Exception {
    when(shedService.getPrediction(any(), any()))
        .thenThrow(new BusinessException(ErrorCode.FEATURE_NOT_SUPPORTED_SPECIES));

    mockMvc
        .perform(MockMvcRequestBuilders.get(BASE + "/prediction").with(asUser()))
        .andExpect(jsonPath("$.error.code").value("FEATURE_NOT_SUPPORTED_SPECIES"));
  }

  @Test
  @DisplayName("[REQ-10-93] 게코 펫의 탈피 예측 조회는 200 이다")
  void req_10_93_geckoPredictionReturnsOk() throws Exception {
    when(shedService.getPrediction(any(), any()))
        .thenReturn(new ShedPredictionResponse(null, null, 0, PredictionConfidence.LOW));

    mockMvc
        .perform(MockMvcRequestBuilders.get(BASE + "/prediction").with(asUser()))
        .andExpect(status().isOk());
  }
}
