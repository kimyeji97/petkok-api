package com.petkok.business.activity.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import com.petkok.business.activity.service.ActivityService;
import com.petkok.data.activity.dto.ActivityCreateRequest;
import com.petkok.data.activity.dto.ActivityResponse;
import com.petkok.data.activity.enums.ActivityType;
import com.petkok.framework.config.JacksonConfig;
import com.petkok.framework.config.SecurityConfig;
import com.petkok.framework.security.AuthPrincipal;
import com.petkok.framework.security.UserStatusChecker;
import com.petkok.framework.security.jwt.JwtTokenProvider;
import java.time.Instant;
import java.time.OffsetDateTime;
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
 * 시각의 <b>HTTP 왕복 계약</b>. 검증 계약 REQ-16-08 · 09 · 15 (PLAN-REQ-16 § 검증 계약). 구성은 AGENTS §6 관례.
 *
 * <p><b>REQ-16-08 은 Phase 2 전까지 실패한다.</b> {@code JacksonConfig} 에 아직 시각 설정이 없다.
 *
 * <p>⭐ <b>응답 픽스처의 오프셋을 {@code Z} 로 두는 것이 이 케이스의 핵심이다.</b> {@code timestamptz} 는 원래 오프셋을 저장하지 않아
 * <b>DB 에서 읽으면 항상 {@code Z} 로 돌아온다</b>(2026-08-28 Phase 0 실측). 픽스처에 {@code +09:00} 을 미리 달아 두면
 * <b>설정이 없어도 통과해</b> 케이스가 아무것도 검증하지 못한다 — 실제로 Phase 0 프로브에서 그렇게 재다가 "설정 불필요"라는 정반대 결론이 나올 뻔했다.
 *
 * <p><b>REQ-16-15 도 Phase 2 전까지 실패할 가능성이 높다.</b> Jackson 기본 동작으로 오프셋 없는 값이 {@code OffsetDateTime}
 * 으로 읽히는지, 읽힌다면 어느 존을 쓰는지는 재 보지 않았다 — Phase 0 프로브가 이것을 다루지 않았다. 계약(D9)은 KST 이고, 구현이 그렇게 되도록 만드는 것이
 * Phase 2 의 일이다.
 *
 * <p>REQ-16-09 는 <b>지금도 통과할 수 있다.</b> Jackson 이 두 표기를 이미 같은 순간으로 읽기 때문이다. 그래도 두는 이유는 회귀 방어다 — 누가
 * {@code ADJUST_DATES_TO_CONTEXT_TIME_ZONE} 을 건드리거나 타입을 {@code LocalDateTime} 으로 되돌리면 이 케이스가 먼저
 * 깨진다.
 */
@WebMvcTest(ActivityController.class)
@Import({SecurityConfig.class, JacksonConfig.class})
class ActivityTimeSerializationWebMvcTest {

  private static final UUID USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
  private static final UUID PET_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
  private static final UUID LOG_ID = UUID.fromString("bbbbbbbb-0000-0000-0000-000000000001");
  private static final String BASE = "/api/v1/pets/" + PET_ID + "/activity";

  /** 2026-06-30T18:00:00+09:00 과 같은 순간. */
  private static final Instant FIXED = Instant.parse("2026-06-30T09:00:00Z");

  private static final String EXPECTED_KST = "2026-06-30T18:00:00+09:00";

  @Autowired private MockMvc mockMvc;
  @MockBean private ActivityService activityService;
  @MockBean private JwtTokenProvider jwtTokenProvider;
  @MockBean private UserStatusChecker userStatusChecker;

  private static RequestPostProcessor asUser() {
    return authentication(
        new UsernamePasswordAuthenticationToken(
            new AuthPrincipal(USER_ID), null, Collections.emptyList()));
  }

  private static String body(String loggedAt) {
    return "{\"activity_type\":\"WALK\",\"logged_at\":\"" + loggedAt + "\"}";
  }

  @Test
  @DisplayName("[REQ-16-08] 응답 시각 필드가 +09:00 오프셋을 달고 나간다")
  void req_16_08_responseTimesCarryKstOffset() throws Exception {
    OffsetDateTime fromDb = FIXED.atOffset(java.time.ZoneOffset.UTC);
    when(activityService.create(any(), any(), any()))
        .thenReturn(
            new ActivityResponse(
                LOG_ID, PET_ID, ActivityType.WALK, 30, null, null, fromDb, fromDb));

    mockMvc
        .perform(
            MockMvcRequestBuilders.post(BASE)
                .with(asUser())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body("2026-06-30T09:00:00Z")))
        .andExpect(jsonPath("$.data.logged_at").value(EXPECTED_KST))
        .andExpect(jsonPath("$.data.created_at").value(EXPECTED_KST));
  }

  @Test
  @DisplayName("[REQ-16-15] 오프셋 없는 요청 값은 KST 로 해석된다")
  void req_16_15_offsetlessValueIsReadAsKst() throws Exception {
    when(activityService.create(any(), any(), any()))
        .thenReturn(
            new ActivityResponse(LOG_ID, PET_ID, ActivityType.WALK, 30, null, null, null, null));

    mockMvc.perform(
        MockMvcRequestBuilders.post(BASE)
            .with(asUser())
            .contentType(MediaType.APPLICATION_JSON)
            .content(body("2026-06-30T18:00:00")));

    ArgumentCaptor<ActivityCreateRequest> captor =
        ArgumentCaptor.forClass(ActivityCreateRequest.class);
    verify(activityService).create(any(), any(), captor.capture());

    assertThat(captor.getValue().loggedAt().toInstant()).isEqualTo(FIXED);
  }

  @Test
  @DisplayName("[REQ-16-09] Z 로 온 요청과 +09:00 으로 온 요청이 같은 순간으로 저장된다")
  void req_16_09_zAndKstOffsetParseToSameInstant() throws Exception {
    when(activityService.create(any(), any(), any()))
        .thenReturn(
            new ActivityResponse(LOG_ID, PET_ID, ActivityType.WALK, 30, null, null, null, null));

    for (String value : new String[] {"2026-06-30T09:00:00Z", "2026-06-30T18:00:00+09:00"}) {
      mockMvc.perform(
          MockMvcRequestBuilders.post(BASE)
              .with(asUser())
              .contentType(MediaType.APPLICATION_JSON)
              .content(body(value)));
    }

    ArgumentCaptor<ActivityCreateRequest> captor =
        ArgumentCaptor.forClass(ActivityCreateRequest.class);
    verify(activityService, times(2)).create(any(), any(), captor.capture());

    assertThat(captor.getAllValues())
        .extracting(r -> r.loggedAt().toInstant())
        .containsExactly(FIXED, FIXED);
  }
}
