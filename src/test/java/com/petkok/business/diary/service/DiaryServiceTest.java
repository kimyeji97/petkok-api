package com.petkok.business.diary.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.petkok.business.pet.service.PetAccessGuard;
import com.petkok.data.diary.dto.DiaryCreateRequest;
import com.petkok.data.diary.dto.DiaryResponse;
import com.petkok.data.diary.dto.DiaryUpdateRequest;
import com.petkok.data.diary.entity.DiaryEntry;
import com.petkok.data.diary.enums.ConditionTag;
import com.petkok.data.diary.repository.DiaryEntryRepository;
import com.petkok.data.pet.dto.OwnedPetResponse;
import com.petkok.data.pet.enums.Species;
import com.petkok.framework.exception.BusinessException;
import com.petkok.framework.exception.ErrorCode;
import com.petkok.framework.pagination.CursorCodec;
import com.petkok.framework.pagination.CursorPage;
import com.petkok.framework.pagination.CursorRequest;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * 다이어리의 <b>가드 위임 · D6 · 커서 · 병합 · 미래 날짜 거부 · condition_tag 필터</b>. 검증 계약 REQ-10-96 ~ 98 · 101 · 102
 * · 105 · 106 · 111 · 112 (PLAN-REQ-10 § 검증 계약). {@code WeightServiceTest} 와 같은 구성 — 날짜 필드가 {@code
 * LocalDate} 지만 "미래 불가" 판정에 KST 기준 "오늘"이 필요해 {@code Clock} 을 쓴다(2026-09-02 확정 — ADR-0002
 * 계산=Asia/Seoul).
 *
 * <p>⚠️ 이 파일은 {@code DiaryService} 등 Phase 5 대상 클래스가 아직 없어 컴파일되지 않는다. {@code /implement REQ-10 5} 가
 * 만든다.
 *
 * <p>가정한 계약 — {@code DiaryService(PetAccessGuard, DiaryEntryRepository, CursorCodec, Clock)}.
 * {@code list} 는 {@code ConditionTag} 필터(nullable)를 추가로 받아, 필터가 있으면 저장소의 필터 전용 쿼리 메서드를 쓴다.
 */
class DiaryServiceTest {

  private static final UUID OWNER = UUID.fromString("11111111-1111-1111-1111-111111111111");
  private static final UUID STRANGER = UUID.fromString("22222222-2222-2222-2222-222222222222");
  private static final UUID PET_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
  private static final UUID ENTRY_A = UUID.fromString("eeeeeeee-0000-0000-0000-000000000003");
  private static final UUID ENTRY_B = UUID.fromString("eeeeeeee-0000-0000-0000-000000000002");
  private static final UUID ENTRY_C = UUID.fromString("eeeeeeee-0000-0000-0000-000000000001");

  // 고정 "오늘" — 2026-07-20 (KST)
  private static final LocalDate TODAY = LocalDate.of(2026, 7, 20);
  private static final LocalDate YESTERDAY = LocalDate.of(2026, 7, 19);
  private static final LocalDate TOMORROW = LocalDate.of(2026, 7, 21);
  private static final Clock CLOCK =
      Clock.fixed(Instant.parse("2026-07-20T03:00:00Z"), ZoneId.of("Asia/Seoul"));

  private final PetAccessGuard guard = mock(PetAccessGuard.class);
  private final DiaryEntryRepository repository = mock(DiaryEntryRepository.class);
  private final CursorCodec codec = new CursorCodec(new ObjectMapper().findAndRegisterModules());
  private final DiaryService service = new DiaryService(guard, repository, codec, CLOCK);

  private static DiaryEntry entry(UUID id, LocalDate entryDate, ConditionTag tag) {
    DiaryEntry entry = DiaryEntry.of(PET_ID, null, null, tag, entryDate);
    ReflectionTestUtils.setField(entry, "id", id);
    return entry;
  }

  private static DiaryCreateRequest create(LocalDate entryDate) {
    return new DiaryCreateRequest(null, null, null, entryDate);
  }

  private void owned() {
    when(guard.getOwnedPet(PET_ID, OWNER))
        .thenReturn(new OwnedPetResponse(PET_ID, Species.CRESTED_GECKO));
    when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));
  }

  // ── 가드 위임 ────────────────────────────────────────────────

  @Test
  @DisplayName("[REQ-10-96] 남의 펫이면 가드의 PET_FORBIDDEN 이 그대로 나간다")
  void req_10_96_strangerGetsForbiddenFromGuard() {
    when(guard.getOwnedPet(PET_ID, STRANGER))
        .thenThrow(new BusinessException(ErrorCode.PET_FORBIDDEN));

    assertThatThrownBy(() -> service.create(STRANGER, PET_ID, create(TODAY)))
        .isInstanceOf(BusinessException.class)
        .extracting(e -> ((BusinessException) e).getErrorCode())
        .isEqualTo(ErrorCode.PET_FORBIDDEN);
  }

  @Test
  @DisplayName("[REQ-10-97] 삭제된 펫이면 가드의 PET_NOT_FOUND 가 그대로 나간다")
  void req_10_97_deletedPetGetsNotFoundFromGuard() {
    when(guard.getOwnedPet(PET_ID, OWNER))
        .thenThrow(new BusinessException(ErrorCode.PET_NOT_FOUND));

    assertThatThrownBy(() -> service.list(OWNER, PET_ID, new CursorRequest(null, 20), null))
        .isInstanceOf(BusinessException.class)
        .extracting(e -> ((BusinessException) e).getErrorCode())
        .isEqualTo(ErrorCode.PET_NOT_FOUND);
  }

  // ── 기록 ↔ 펫 귀속 (D6) ─────────────────────────────────────

  @Test
  @DisplayName("[REQ-10-98] 다른 펫에 속한 기록 id 는 RESOURCE_NOT_FOUND 다")
  void req_10_98_recordOfAnotherPetIsNotFound() {
    owned();
    when(repository.findByIdAndPetId(ENTRY_A, PET_ID)).thenReturn(Optional.empty());

    assertThatThrownBy(
            () ->
                service.update(
                    OWNER, PET_ID, ENTRY_A, new DiaryUpdateRequest(null, "m", null, null)))
        .isInstanceOf(BusinessException.class)
        .extracting(e -> ((BusinessException) e).getErrorCode())
        .isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);
  }

  // ── PATCH 병합 (D10) ────────────────────────────────────────

  @Test
  @DisplayName("[REQ-10-101] content 만 보내면 title 이 유지된다")
  void req_10_101_contentOnlyPatchKeepsTitle() {
    owned();
    DiaryEntry entry = DiaryEntry.of(PET_ID, "원래 제목", "원래 내용", null, TODAY);
    ReflectionTestUtils.setField(entry, "id", ENTRY_A);
    when(repository.findByIdAndPetId(ENTRY_A, PET_ID)).thenReturn(Optional.of(entry));

    service.update(OWNER, PET_ID, ENTRY_A, new DiaryUpdateRequest(null, "새 내용", null, null));

    assertThat(entry.getTitle()).isEqualTo("원래 제목");
  }

  // ── 응답 필드 (D11) ─────────────────────────────────────────

  @Test
  @DisplayName("[REQ-10-107] 응답에 updated_at 이 있다")
  void req_10_107_responseHasUpdatedAt() {
    owned();
    OffsetDateTime updatedAt = OffsetDateTime.parse("2026-07-20T12:00:00+09:00");
    DiaryEntry entry = DiaryEntry.of(PET_ID, "원래 제목", "원래 내용", null, TODAY);
    ReflectionTestUtils.setField(entry, "id", ENTRY_A);
    ReflectionTestUtils.setField(entry, "updatedAt", updatedAt);
    when(repository.findByIdAndPetId(ENTRY_A, PET_ID)).thenReturn(Optional.of(entry));

    DiaryResponse response =
        service.update(OWNER, PET_ID, ENTRY_A, new DiaryUpdateRequest(null, "새 내용", null, null));

    assertThat(response.updatedAt()).isEqualTo(updatedAt);
  }

  // ── keyset 커서 (D8) ────────────────────────────────────────

  @Test
  @DisplayName("[REQ-10-102] next_cursor 페이로드에 마지막 항목의 id 가 실린다")
  void req_10_102_nextCursorCarriesLastItemId() {
    owned();
    when(repository.findFirstPage(eq(PET_ID), any()))
        .thenReturn(
            List.of(
                entry(ENTRY_A, TODAY, null),
                entry(ENTRY_B, TODAY, null),
                entry(ENTRY_C, TODAY, null)));

    CursorPage<DiaryResponse> page = service.list(OWNER, PET_ID, new CursorRequest(null, 2), null);

    assertThat(codec.decode(page.nextCursor(), DiaryCursor.class))
        .isEqualTo(new DiaryCursor(TODAY, ENTRY_B));
  }

  @Test
  @DisplayName("[REQ-10-102] 다음 페이지 조회는 entry_date 와 id 를 둘 다 저장소에 넘긴다")
  void req_10_102_nextPagePassesBothKeysToRepository() {
    owned();
    when(repository.findPageAfter(any(), any(), any(), any())).thenReturn(List.of());

    service.list(
        OWNER, PET_ID, new CursorRequest(codec.encode(new DiaryCursor(TODAY, ENTRY_B)), 2), null);

    verify(repository).findPageAfter(eq(PET_ID), eq(TODAY), eq(ENTRY_B), any());
  }

  // ── 미래 날짜 거부 (미결 질문 Phase 5, KST 자정) ────────────

  @Test
  @DisplayName("[REQ-10-105] entry_date 가 내일(미래)이면 거부된다")
  void req_10_105_futureEntryDateIsRejected() {
    owned();

    assertThatThrownBy(() -> service.create(OWNER, PET_ID, create(TOMORROW)))
        .isInstanceOf(BusinessException.class)
        .extracting(e -> ((BusinessException) e).getErrorCode())
        .isEqualTo(ErrorCode.INVALID_INPUT);
  }

  @Test
  @DisplayName("[REQ-10-106] entry_date 가 오늘이면 정상 저장된다")
  void req_10_106_todayEntryDateIsAccepted() {
    owned();

    assertThatCode(() -> service.create(OWNER, PET_ID, create(TODAY))).doesNotThrowAnyException();
  }

  @Test
  @DisplayName("[REQ-10-106] entry_date 가 어제(과거)면 정상 저장된다")
  void req_10_106_pastEntryDateIsAccepted() {
    owned();

    assertThatCode(() -> service.create(OWNER, PET_ID, create(YESTERDAY)))
        .doesNotThrowAnyException();
  }

  // ── condition_tag 필터 ──────────────────────────────────────

  @Test
  @DisplayName("[REQ-10-111] 필터가 걸린 목록도 다음 페이지 조회에 entry_date·id 를 둘 다 넘긴다")
  void req_10_111_filteredListKeepsKeysetOnNextPage() {
    owned();
    when(repository.findPageAfterByConditionTag(any(), any(), any(), any(), any()))
        .thenReturn(List.of());

    service.list(
        OWNER,
        PET_ID,
        new CursorRequest(codec.encode(new DiaryCursor(TODAY, ENTRY_B)), 2),
        ConditionTag.FLOPPY_TAIL);

    verify(repository)
        .findPageAfterByConditionTag(
            eq(PET_ID), eq(ConditionTag.FLOPPY_TAIL), eq(TODAY), eq(ENTRY_B), any());
  }

  @Test
  @DisplayName("[REQ-10-112] condition_tag 필터를 걸면 저장소의 필터 전용 쿼리 결과만 돌려준다")
  void req_10_112_filterUsesConditionTagQuery() {
    owned();
    when(repository.findFirstPageByConditionTag(eq(PET_ID), eq(ConditionTag.FLOPPY_TAIL), any()))
        .thenReturn(List.of(entry(ENTRY_A, TODAY, ConditionTag.FLOPPY_TAIL)));

    CursorPage<DiaryResponse> page =
        service.list(OWNER, PET_ID, new CursorRequest(null, 20), ConditionTag.FLOPPY_TAIL);

    assertThat(page.items()).hasSize(1);
  }
}
