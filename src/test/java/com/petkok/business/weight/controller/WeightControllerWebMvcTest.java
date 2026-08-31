package com.petkok.business.weight.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.petkok.business.weight.service.WeightService;
import com.petkok.data.weight.dto.WeightResponse;
import com.petkok.framework.config.JacksonConfig;
import com.petkok.framework.config.SecurityConfig;
import com.petkok.framework.pagination.CursorPage;
import com.petkok.framework.pagination.CursorRequest;
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
 * 체중 기록의 <b>HTTP 계약</b>. 검증 계약 REQ-10-04 · 05 · 11 ~ 14 · 22 (PLAN-REQ-10 § 검증 계약).
 *
 * <p>설정은 AGENTS §6 관례 — {@code @Import({SecurityConfig, JacksonConfig})} · {@link
 * UserStatusChecker} 는 이 슬라이스에 {@code UserService} 가 없으므로 따로 목. {@code message} 는 단언하지 않는다.
 */
@WebMvcTest(WeightController.class)
@Import({SecurityConfig.class, JacksonConfig.class})
class WeightControllerWebMvcTest {

  private static final UUID USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
  private static final UUID PET_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
  private static final UUID LOG_ID = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000001");
  private static final String BASE = "/api/v1/pets/" + PET_ID + "/weight";

  @Autowired private MockMvc mockMvc;
  @MockBean private WeightService weightService;
  @MockBean private JwtTokenProvider jwtTokenProvider;
  @MockBean private UserStatusChecker userStatusChecker;

  private static RequestPostProcessor asUser() {
    return authentication(
        new UsernamePasswordAuthenticationToken(
            new AuthPrincipal(USER_ID), null, Collections.emptyList()));
  }

  private static WeightResponse sample() {
    return new WeightResponse(
        LOG_ID, PET_ID, 62, LocalDate.of(2026, 6, 30), null, null, false, OffsetDateTime.now());
  }

  @Test
  @DisplayName("[REQ-10-04] POST /weight 는 201 을 반환한다")
  void req_10_04_createReturnsCreated() throws Exception {
    when(weightService.create(any(), any(), any())).thenReturn(sample());

    mockMvc
        .perform(
            MockMvcRequestBuilders.post(BASE)
                .with(asUser())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"weight_g\":62,\"measured_at\":\"2026-06-30\"}"))
        .andExpect(status().isCreated());
  }

  @Test
  @DisplayName("[REQ-10-05] DELETE /weight/{log_id} 는 204 다")
  void req_10_05_deleteReturnsNoContent() throws Exception {
    mockMvc
        .perform(MockMvcRequestBuilders.delete(BASE + "/" + LOG_ID).with(asUser()))
        .andExpect(status().isNoContent());
  }

  @Test
  @DisplayName("[REQ-10-05] DELETE /weight/{log_id} 는 본문이 비어 있다")
  void req_10_05_deleteHasEmptyBody() throws Exception {
    mockMvc
        .perform(MockMvcRequestBuilders.delete(BASE + "/" + LOG_ID).with(asUser()))
        .andExpect(content().string(""));
  }

  @Test
  @DisplayName("[REQ-10-11] GET /weight 응답에 items · next_cursor · has_next 키가 있다")
  void req_10_11_listResponseHasCursorPageKeys() throws Exception {
    when(weightService.list(any(), any(), any()))
        .thenReturn(CursorPage.of(List.of(sample()), null, false));

    mockMvc
        .perform(MockMvcRequestBuilders.get(BASE).with(asUser()))
        .andExpect(jsonPath("$.data.items").isArray())
        .andExpect(jsonPath("$.data.next_cursor").hasJsonPath())
        .andExpect(jsonPath("$.data.has_next").value(false));
  }

  @Test
  @DisplayName("[REQ-10-11] 목록 항목의 파생 필드 키가 snake_case 다")
  void req_10_11_itemKeysAreSnakeCase() throws Exception {
    when(weightService.list(any(), any(), any()))
        .thenReturn(CursorPage.of(List.of(sample()), null, false));

    mockMvc
        .perform(MockMvcRequestBuilders.get(BASE).with(asUser()))
        .andExpect(jsonPath("$.data.items[0].is_weight_warning").value(false))
        .andExpect(jsonPath("$.data.items[0].weight_change_rate").hasJsonPath());
  }

  @Test
  @DisplayName("[REQ-10-12] weight_g 가 0 이면 400 이다")
  void req_10_12_zeroWeightIsRejected() throws Exception {
    mockMvc
        .perform(
            MockMvcRequestBuilders.post(BASE)
                .with(asUser())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"weight_g\":0,\"measured_at\":\"2026-06-30\"}"))
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("[REQ-10-13] weight_g 가 없으면 400 이다")
  void req_10_13_missingWeightIsRejected() throws Exception {
    mockMvc
        .perform(
            MockMvcRequestBuilders.post(BASE)
                .with(asUser())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"measured_at\":\"2026-06-30\"}"))
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("[REQ-10-14] measured_at 이 없으면 400 이다")
  void req_10_14_missingMeasuredAtIsRejected() throws Exception {
    mockMvc
        .perform(
            MockMvcRequestBuilders.post(BASE)
                .with(asUser())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"weight_g\":62}"))
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("[REQ-10-22] limit 을 안 보내면 서비스가 받는 limit 은 20 이다")
  void req_10_22_defaultLimitIsTwenty() throws Exception {
    when(weightService.list(any(), any(), any())).thenReturn(CursorPage.of(List.of(), null, false));

    mockMvc.perform(MockMvcRequestBuilders.get(BASE).with(asUser())).andExpect(status().isOk());

    ArgumentCaptor<CursorRequest> captor = ArgumentCaptor.forClass(CursorRequest.class);
    verify(weightService).list(any(), any(), captor.capture());
    assertThat(captor.getValue().limit()).isEqualTo(20);
  }

  @Test
  @DisplayName("[REQ-10-22] cursor 쿼리 파라미터가 서비스에 그대로 전달된다")
  void req_10_22_cursorParamIsPassedThrough() throws Exception {
    when(weightService.list(any(), any(), any())).thenReturn(CursorPage.of(List.of(), null, false));

    mockMvc
        .perform(MockMvcRequestBuilders.get(BASE).param("cursor", "abc").with(asUser()))
        .andExpect(status().isOk());

    ArgumentCaptor<CursorRequest> captor = ArgumentCaptor.forClass(CursorRequest.class);
    verify(weightService).list(any(), any(), captor.capture());
    assertThat(captor.getValue().cursor()).isEqualTo("abc");
  }
}
