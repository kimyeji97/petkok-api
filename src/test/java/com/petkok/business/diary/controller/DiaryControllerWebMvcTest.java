package com.petkok.business.diary.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.petkok.business.diary.service.DiaryService;
import com.petkok.data.diary.dto.DiaryCreateRequest;
import com.petkok.data.diary.dto.DiaryResponse;
import com.petkok.data.diary.enums.ConditionTag;
import com.petkok.framework.config.JacksonConfig;
import com.petkok.framework.config.SecurityConfig;
import com.petkok.framework.pagination.CursorPage;
import com.petkok.framework.port.PhotoSummary;
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
 * 다이어리의 <b>HTTP 계약</b>. 검증 계약 REQ-10-94 · 95 · 99 · 103 · 104 · 108 ~ 110 · 113 · 114 (PLAN-REQ-10
 * § 검증 계약). 구성은 AGENTS §6 관례.
 *
 * <p>⚠️ 이 파일은 {@code DiaryController} 등이 아직 없어 컴파일되지 않는다 — {@code /implement REQ-10 5} 가 만든다.
 *
 * <p>REQ-10-108·109는 REQ-11 Phase 2(다이어리↔사진 연결)에서 단언이 뒤집혔다 — 원래 "필드 없음"이었던 것이 "필드 있고 값이 채워짐"으로
 * 바뀌었다. ID는 그대로 유지한다(PLAN-REQ-11 § 검증 계약 참고). REQ-10-110(photo_ids 무시)은 그대로다.
 */
@WebMvcTest(DiaryController.class)
@Import({SecurityConfig.class, JacksonConfig.class})
class DiaryControllerWebMvcTest {

  private static final UUID USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
  private static final UUID PET_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
  private static final UUID ENTRY_ID = UUID.fromString("eeeeeeee-0000-0000-0000-000000000001");
  private static final String BASE = "/api/v1/pets/" + PET_ID + "/diary";

  @Autowired private MockMvc mockMvc;
  @MockBean private DiaryService diaryService;
  @MockBean private JwtTokenProvider jwtTokenProvider;
  @MockBean private UserStatusChecker userStatusChecker;

  private static RequestPostProcessor asUser() {
    return authentication(
        new UsernamePasswordAuthenticationToken(
            new AuthPrincipal(USER_ID), null, Collections.emptyList()));
  }

  private static DiaryResponse sample(ConditionTag tag) {
    return sample(tag, null, null);
  }

  private static DiaryResponse sample(
      ConditionTag tag, List<PhotoSummary> photos, Integer photoCount) {
    return new DiaryResponse(
        ENTRY_ID,
        PET_ID,
        "제목",
        "내용",
        tag,
        LocalDate.of(2026, 7, 20),
        OffsetDateTime.now(),
        OffsetDateTime.now(),
        photos,
        photoCount);
  }

  @Test
  @DisplayName("[REQ-10-94] POST /diary 는 201 을 반환한다")
  void req_10_94_createReturnsCreated() throws Exception {
    when(diaryService.create(any(), any(), any())).thenReturn(sample(null));

    mockMvc
        .perform(
            MockMvcRequestBuilders.post(BASE)
                .with(asUser())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"entry_date\":\"2026-07-20\"}"))
        .andExpect(status().isCreated());
  }

  @Test
  @DisplayName("[REQ-10-95] DELETE /diary/{entry_id} 는 204 다")
  void req_10_95_deleteReturnsNoContent() throws Exception {
    mockMvc
        .perform(MockMvcRequestBuilders.delete(BASE + "/" + ENTRY_ID).with(asUser()))
        .andExpect(status().isNoContent());
  }

  @Test
  @DisplayName("[REQ-10-95] DELETE /diary/{entry_id} 는 본문이 비어 있다")
  void req_10_95_deleteHasEmptyBody() throws Exception {
    mockMvc
        .perform(MockMvcRequestBuilders.delete(BASE + "/" + ENTRY_ID).with(asUser()))
        .andExpect(content().string(""));
  }

  @Test
  @DisplayName("[REQ-10-99] GET /diary 응답에 items · next_cursor · has_next 키가 있다")
  void req_10_99_listResponseHasCursorPageKeys() throws Exception {
    when(diaryService.list(any(), any(), any(), any()))
        .thenReturn(CursorPage.of(List.of(sample(null)), null, false));

    mockMvc
        .perform(MockMvcRequestBuilders.get(BASE).with(asUser()))
        .andExpect(jsonPath("$.data.items").isArray())
        .andExpect(jsonPath("$.data.next_cursor").hasJsonPath())
        .andExpect(jsonPath("$.data.has_next").value(false));
  }

  @Test
  @DisplayName("[REQ-10-103] entry_date 가 없으면 400 이다")
  void req_10_103_missingEntryDateIsRejected() throws Exception {
    mockMvc
        .perform(
            MockMvcRequestBuilders.post(BASE)
                .with(asUser())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("[REQ-10-104] condition_tag 가 \"거식\" 이면 400 이다")
  void req_10_104_oldSevenValueTagIsRejected() throws Exception {
    mockMvc
        .perform(
            MockMvcRequestBuilders.post(BASE)
                .with(asUser())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"entry_date\":\"2026-07-20\",\"condition_tag\":\"거식\"}"))
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("[REQ-10-108] 상세 응답에 photos 배열이 있다 (REQ-11 Phase 2 — 뒤집힘)")
  void req_10_108_responseHasPhotosField() throws Exception {
    List<PhotoSummary> photos =
        List.of(
            new PhotoSummary(
                UUID.fromString("aaaaaaaa-0000-0000-0000-000000000009"),
                "https://img.petkok.com/photos/a.jpg",
                null,
                null));
    when(diaryService.create(any(), any(), any())).thenReturn(sample(null, photos, null));

    mockMvc
        .perform(
            MockMvcRequestBuilders.post(BASE)
                .with(asUser())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"entry_date\":\"2026-07-20\"}"))
        .andExpect(jsonPath("$.data.photos").isArray())
        .andExpect(
            jsonPath("$.data.photos[0].image_url").value("https://img.petkok.com/photos/a.jpg"));
  }

  @Test
  @DisplayName("[REQ-10-109] 목록 항목에 photo_count 필드가 있다 (REQ-11 Phase 2 — 뒤집힘)")
  void req_10_109_listItemHasPhotoCountField() throws Exception {
    when(diaryService.list(any(), any(), any(), any()))
        .thenReturn(CursorPage.of(List.of(sample(null, null, 3)), null, false));

    mockMvc
        .perform(MockMvcRequestBuilders.get(BASE).with(asUser()))
        .andExpect(jsonPath("$.data.items[0].photo_count").value(3));
  }

  @Test
  @DisplayName("[REQ-10-110] photo_ids 를 보내도 무시되고 201 이다")
  void req_10_110_photoIdsAreIgnored() throws Exception {
    when(diaryService.create(any(), any(), any())).thenReturn(sample(null));

    mockMvc
        .perform(
            MockMvcRequestBuilders.post(BASE)
                .with(asUser())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"entry_date\":\"2026-07-20\",\"photo_ids\":[\"aaaaaaaa-0000-0000-0000-000000000001\"]}"))
        .andExpect(status().isCreated());
  }

  @Test
  @DisplayName("[REQ-10-113] condition_tag \"정상\" 요청이 NORMAL 로 파싱돼 서비스에 전달된다")
  void req_10_113_koreanConditionTagParsesToEnum() throws Exception {
    when(diaryService.create(any(), any(), any())).thenReturn(sample(ConditionTag.NORMAL));

    mockMvc
        .perform(
            MockMvcRequestBuilders.post(BASE)
                .with(asUser())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"entry_date\":\"2026-07-20\",\"condition_tag\":\"정상\"}"))
        .andExpect(status().isCreated());

    ArgumentCaptor<DiaryCreateRequest> captor = ArgumentCaptor.forClass(DiaryCreateRequest.class);
    org.mockito.Mockito.verify(diaryService).create(any(), any(), captor.capture());
    org.assertj.core.api.Assertions.assertThat(captor.getValue().conditionTag())
        .isEqualTo(ConditionTag.NORMAL);
  }

  @Test
  @DisplayName("[REQ-10-114] condition_tag 응답은 한글로 직렬화된다 (NORMAL → 정상)")
  void req_10_114_conditionTagSerializesToKorean() throws Exception {
    when(diaryService.create(any(), any(), any())).thenReturn(sample(ConditionTag.NORMAL));

    mockMvc
        .perform(
            MockMvcRequestBuilders.post(BASE)
                .with(asUser())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"entry_date\":\"2026-07-20\",\"condition_tag\":\"정상\"}"))
        .andExpect(jsonPath("$.data.condition_tag").value("정상"));
  }
}
