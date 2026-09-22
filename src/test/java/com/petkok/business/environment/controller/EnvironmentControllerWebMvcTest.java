package com.petkok.business.environment.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.petkok.business.environment.service.EnvironmentService;
import com.petkok.data.environment.dto.EnvironmentDailySummaryResponse;
import com.petkok.data.environment.dto.EnvironmentResponse;
import com.petkok.framework.config.JacksonConfig;
import com.petkok.framework.config.SecurityConfig;
import com.petkok.framework.pagination.CursorPage;
import com.petkok.framework.security.AuthPrincipal;
import com.petkok.framework.security.UserStatusChecker;
import com.petkok.framework.security.jwt.JwtTokenProvider;
import java.math.BigDecimal;
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
 * 게코 사육 환경(온습도) 기록의 <b>HTTP 계약</b>. 검증 계약 REQ-20-04 ~ 08 · 14 · 15 (PLAN-REQ-20 § 검증 계약). 구성은
 * AGENTS §6 관례.
 *
 * <p>⚠️ 이 파일은 {@code EnvironmentController} 등이 아직 없어 컴파일되지 않는다 — {@code /implement REQ-20 1} 이 만든다.
 */
@WebMvcTest(EnvironmentController.class)
@Import({SecurityConfig.class, JacksonConfig.class})
class EnvironmentControllerWebMvcTest {

  private static final UUID USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
  private static final UUID PET_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
  private static final UUID LOG_ID = UUID.fromString("eeeeeeee-0000-0000-0000-000000000001");
  private static final String BASE = "/api/v1/pets/" + PET_ID + "/environment";

  @Autowired private MockMvc mockMvc;
  @MockBean private EnvironmentService environmentService;
  @MockBean private JwtTokenProvider jwtTokenProvider;
  @MockBean private UserStatusChecker userStatusChecker;

  private static RequestPostProcessor asUser() {
    return authentication(
        new UsernamePasswordAuthenticationToken(
            new AuthPrincipal(USER_ID), null, Collections.emptyList()));
  }

  private static EnvironmentResponse sample() {
    return new EnvironmentResponse(
        LOG_ID,
        PET_ID,
        new BigDecimal("24.5"),
        new BigDecimal("65.0"),
        OffsetDateTime.now(),
        null,
        OffsetDateTime.now());
  }

  @Test
  @DisplayName("[REQ-20-04] POST /environment 는 201 을 반환한다")
  void req_20_04_createReturnsCreated() throws Exception {
    when(environmentService.create(any(), any(), any())).thenReturn(sample());

    mockMvc
        .perform(
            MockMvcRequestBuilders.post(BASE)
                .with(asUser())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"temperature\":24.5,\"humidity\":65.0,\"measured_at\":\"2026-09-22T10:00:00+09:00\"}"))
        .andExpect(status().isCreated());
  }

  @Test
  @DisplayName("[REQ-20-05] GET /environment 는 200 을 반환한다")
  void req_20_05_listReturnsOk() throws Exception {
    when(environmentService.list(any(), any(), any()))
        .thenReturn(CursorPage.of(List.of(sample()), null, false));

    mockMvc.perform(MockMvcRequestBuilders.get(BASE).with(asUser())).andExpect(status().isOk());
  }

  @Test
  @DisplayName("[REQ-20-06] PATCH /environment/{id} 는 200 을 반환한다")
  void req_20_06_updateReturnsOk() throws Exception {
    when(environmentService.update(any(), any(), any(), any())).thenReturn(sample());

    mockMvc
        .perform(
            MockMvcRequestBuilders.patch(BASE + "/" + LOG_ID)
                .with(asUser())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"memo\":\"환기 후 재측정\"}"))
        .andExpect(status().isOk());
  }

  @Test
  @DisplayName("[REQ-20-07] DELETE /environment/{id} 는 204 다")
  void req_20_07_deleteReturnsNoContent() throws Exception {
    mockMvc
        .perform(MockMvcRequestBuilders.delete(BASE + "/" + LOG_ID).with(asUser()))
        .andExpect(status().isNoContent());
  }

  @Test
  @DisplayName("[REQ-20-08] DELETE /environment/{id} 는 본문이 비어 있다")
  void req_20_08_deleteHasEmptyBody() throws Exception {
    mockMvc
        .perform(MockMvcRequestBuilders.delete(BASE + "/" + LOG_ID).with(asUser()))
        .andExpect(content().string(""));
  }

  @Test
  @DisplayName("[REQ-20-14] GET /environment/daily-summary 는 200 을 반환한다")
  void req_20_14_dailySummaryReturnsOk() throws Exception {
    when(environmentService.getDailySummary(any(), any(), any()))
        .thenReturn(new EnvironmentDailySummaryResponse(new BigDecimal("25.0"), null, 0));

    mockMvc
        .perform(
            MockMvcRequestBuilders.get(BASE + "/daily-summary")
                .param("date", "2026-09-22")
                .with(asUser()))
        .andExpect(status().isOk());
  }

  @Test
  @DisplayName("[REQ-20-15] daily-summary 응답에 avg_temperature·avg_humidity·record_count 키가 있다")
  void req_20_15_dailySummaryResponseHasExpectedKeys() throws Exception {
    when(environmentService.getDailySummary(any(), any(), any()))
        .thenReturn(
            new EnvironmentDailySummaryResponse(new BigDecimal("25.0"), new BigDecimal("62.5"), 4));

    mockMvc
        .perform(
            MockMvcRequestBuilders.get(BASE + "/daily-summary")
                .param("date", "2026-09-22")
                .with(asUser()))
        .andExpect(jsonPath("$.data.avg_temperature").value(25.0))
        .andExpect(jsonPath("$.data.avg_humidity").value(62.5))
        .andExpect(jsonPath("$.data.record_count").value(4));
  }
}
