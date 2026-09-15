package com.petkok.business.timeline.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.petkok.business.timeline.service.TimelineService;
import com.petkok.data.diary.enums.ConditionTag;
import com.petkok.data.timeline.dto.TimelineDayResponse;
import com.petkok.data.timeline.dto.TimelineEventResponse;
import com.petkok.data.timeline.dto.TimelineResponse;
import com.petkok.data.timeline.enums.TimelineType;
import com.petkok.framework.config.JacksonConfig;
import com.petkok.framework.config.SecurityConfig;
import com.petkok.framework.exception.BusinessException;
import com.petkok.framework.exception.ErrorCode;
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/**
 * timeline 월간 집계의 <b>HTTP 계약</b>. 검증 계약 REQ-12-27 ~ 32 (PLAN-REQ-12 § 검증 계약).
 *
 * <p>설정은 AGENTS §6 관례 — {@code @Import({SecurityConfig, JacksonConfig})} · {@link
 * UserStatusChecker} 는 이 슬라이스에 {@code UserService} 가 없으므로 따로 목. {@code message} 는 단언하지 않는다.
 *
 * <p>⚠️ 이 파일은 {@code TimelineController} 가 아직 없어 컴파일되지 않는다 — {@code /implement REQ-12 1} 이 만든다.
 *
 * <p><b>가정한 계약</b> — 컨트롤러가 {@code year_month} 문자열은 {@code YearMonth.parse}, {@code type} 문자열은
 * {@code TimelineType.fromParam}(원본 그대로의 소문자 값 허용, 미지정 시 {@code all})으로 <b>직접 변환</b>해 서비스에 넘긴다 —
 * {@code WeightController} 가 {@code cursor}·{@code limit} 을 {@code CursorRequest} 로 감싸 넘기는 것과 같은
 * 위치의 책임이다. 이 변환 자체(대소문자·기본값)는 이 컨트롤러 테스트가, 필터링 로직은 {@code TimelineMergerTest} 가 검증한다.
 */
@WebMvcTest(TimelineController.class)
@Import({SecurityConfig.class, JacksonConfig.class})
class TimelineControllerWebMvcTest {

  private static final UUID USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
  private static final UUID PET_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
  private static final UUID EVENT_ID = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000001");
  private static final String BASE = "/api/v1/pets/" + PET_ID + "/timeline";

  @Autowired private MockMvc mockMvc;
  @MockBean private TimelineService timelineService;
  @MockBean private JwtTokenProvider jwtTokenProvider;
  @MockBean private UserStatusChecker userStatusChecker;

  private static RequestPostProcessor asUser() {
    return authentication(
        new UsernamePasswordAuthenticationToken(
            new AuthPrincipal(USER_ID), null, Collections.emptyList()));
  }

  private static TimelineResponse sample() {
    TimelineEventResponse event =
        new TimelineEventResponse(
            "diary",
            EVENT_ID,
            OffsetDateTime.parse("2026-06-30T00:00:00+09:00"),
            "오늘의 두부",
            ConditionTag.ACTIVE,
            null,
            null,
            null,
            null);
    TimelineDayResponse day =
        new TimelineDayResponse(LocalDate.of(2026, 6, 30), List.of("diary"), List.of(event));
    return new TimelineResponse(List.of(day));
  }

  @Test
  @DisplayName("[REQ-12-27] GET /timeline 은 200 과 data.days 배열을 반환한다")
  void req_12_27_listReturnsOkWithDaysArray() throws Exception {
    when(timelineService.list(any(), any(), any(), any())).thenReturn(sample());

    mockMvc
        .perform(MockMvcRequestBuilders.get(BASE).param("year_month", "2026-06").with(asUser()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.days").isArray());
  }

  @Test
  @DisplayName("[REQ-12-28] type 파라미터를 안 보내면 서비스는 TimelineType.ALL 을 받는다")
  void req_12_28_missingTypeDefaultsToAll() throws Exception {
    when(timelineService.list(any(), any(), any(), any())).thenReturn(sample());

    mockMvc
        .perform(MockMvcRequestBuilders.get(BASE).param("year_month", "2026-06").with(asUser()))
        .andExpect(status().isOk());

    ArgumentCaptor<TimelineType> captor = ArgumentCaptor.forClass(TimelineType.class);
    verify(timelineService).list(any(), any(), any(), captor.capture());
    assertThat(captor.getValue()).isEqualTo(TimelineType.ALL);
  }

  @Test
  @DisplayName("[REQ-12-29] type=weight(소문자)면 서비스는 TimelineType.WEIGHT 를 받는다")
  void req_12_29_lowercaseTypeParamMapsToEnum() throws Exception {
    when(timelineService.list(any(), any(), any(), any())).thenReturn(sample());

    mockMvc
        .perform(
            MockMvcRequestBuilders.get(BASE)
                .param("year_month", "2026-06")
                .param("type", "weight")
                .with(asUser()))
        .andExpect(status().isOk());

    ArgumentCaptor<TimelineType> captor = ArgumentCaptor.forClass(TimelineType.class);
    verify(timelineService).list(any(), any(), any(), captor.capture());
    assertThat(captor.getValue()).isEqualTo(TimelineType.WEIGHT);
  }

  @Test
  @DisplayName("[REQ-12-30] 남의 펫은 HTTP 왕복에서 403·error.code가 PET_FORBIDDEN이다")
  void req_12_30_strangerGetsForbidden() throws Exception {
    when(timelineService.list(any(), any(), any(), any()))
        .thenThrow(new BusinessException(ErrorCode.PET_FORBIDDEN));

    mockMvc
        .perform(MockMvcRequestBuilders.get(BASE).param("year_month", "2026-06").with(asUser()))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.error.code").value("PET_FORBIDDEN"));
  }

  @Test
  @DisplayName("[REQ-12-31] 없는 펫은 HTTP 왕복에서 404·error.code가 PET_NOT_FOUND다")
  void req_12_31_missingPetGetsNotFound() throws Exception {
    when(timelineService.list(any(), any(), any(), any()))
        .thenThrow(new BusinessException(ErrorCode.PET_NOT_FOUND));

    mockMvc
        .perform(MockMvcRequestBuilders.get(BASE).param("year_month", "2026-06").with(asUser()))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.error.code").value("PET_NOT_FOUND"));
  }

  @Test
  @DisplayName("[REQ-12-32] 응답 이벤트 키는 snake_case다(ref_id · occurred_at)")
  void req_12_32_eventKeysAreSnakeCase() throws Exception {
    when(timelineService.list(any(), any(), any(), any())).thenReturn(sample());

    mockMvc
        .perform(MockMvcRequestBuilders.get(BASE).param("year_month", "2026-06").with(asUser()))
        .andExpect(jsonPath("$.data.days[0].events[0].ref_id").value(EVENT_ID.toString()))
        .andExpect(jsonPath("$.data.days[0].events[0].occurred_at").exists())
        .andExpect(jsonPath("$.data.days[0].events[0].condition_tag").value("활발"));
  }
}
