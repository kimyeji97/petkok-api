package com.petkok.business.shed.service;

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
import com.petkok.data.pet.dto.OwnedPetResponse;
import com.petkok.data.pet.enums.Species;
import com.petkok.data.shed.dto.ShedCreateRequest;
import com.petkok.data.shed.dto.ShedResponse;
import com.petkok.data.shed.dto.ShedUpdateRequest;
import com.petkok.data.shed.entity.ShedRecord;
import com.petkok.data.shed.repository.ShedRecordRepository;
import com.petkok.framework.exception.BusinessException;
import com.petkok.framework.exception.ErrorCode;
import com.petkok.framework.pagination.CursorCodec;
import com.petkok.framework.pagination.CursorPage;
import com.petkok.framework.pagination.CursorRequest;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * 탈피 기록의 <b>가드 위임 · D6 · 커서 · 병합 · 종별 제한</b>. 검증 계약 REQ-10-70 ~ 72 · 75 · 76 · 78 · 83 (PLAN-REQ-10
 * § 검증 계약). {@code WeightServiceTest} 와 같은 구성 — 날짜 필드가 {@code LocalDate} 라 시각·{@code Clock} 이 필요
 * 없다.
 *
 * <p>⚠️ 이 파일은 {@code ShedService} 등 Phase 4 대상 클래스가 아직 없어 컴파일되지 않는다. {@code /implement REQ-10 4} 가
 * 만든다.
 *
 * <p>가정한 계약 — {@code ShedService(PetAccessGuard, ShedRecordRepository, CursorCodec)}. 종별 제한은 {@code
 * create}·{@code list}·{@code update}·{@code delete} <b>네 메서드 전부</b> 진입 시 적용하고 {@code
 * ErrorCode.FEATURE_NOT_SUPPORTED_SPECIES} 를 던진다(api-list.md § 8).
 */
class ShedServiceTest {

  private static final UUID OWNER = UUID.fromString("11111111-1111-1111-1111-111111111111");
  private static final UUID STRANGER = UUID.fromString("22222222-2222-2222-2222-222222222222");
  private static final UUID PET_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
  private static final UUID RECORD_A = UUID.fromString("dddddddd-0000-0000-0000-000000000003");
  private static final UUID RECORD_B = UUID.fromString("dddddddd-0000-0000-0000-000000000002");
  private static final UUID RECORD_C = UUID.fromString("dddddddd-0000-0000-0000-000000000001");
  private static final LocalDate JUL_20 = LocalDate.of(2026, 7, 20);
  private static final LocalDate JUN_20 = LocalDate.of(2026, 6, 20);

  private final PetAccessGuard guard = mock(PetAccessGuard.class);
  private final ShedRecordRepository repository = mock(ShedRecordRepository.class);
  private final CursorCodec codec = new CursorCodec(new ObjectMapper().findAndRegisterModules());
  private final ShedService service = new ShedService(guard, repository, codec);

  private static ShedRecord record(UUID id, LocalDate shedDate) {
    ShedRecord record = ShedRecord.of(PET_ID, shedDate, true, false, null);
    ReflectionTestUtils.setField(record, "id", id);
    return record;
  }

  private static ShedCreateRequest create(LocalDate shedDate) {
    return new ShedCreateRequest(shedDate, null, null, null);
  }

  private void owned(Species species) {
    when(guard.getOwnedPet(PET_ID, OWNER)).thenReturn(new OwnedPetResponse(PET_ID, species));
    when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));
  }

  // ── 가드 위임 ────────────────────────────────────────────────

  @Test
  @DisplayName("[REQ-10-70] 남의 펫이면 가드의 PET_FORBIDDEN 이 그대로 나간다")
  void req_10_70_strangerGetsForbiddenFromGuard() {
    when(guard.getOwnedPet(PET_ID, STRANGER))
        .thenThrow(new BusinessException(ErrorCode.PET_FORBIDDEN));

    assertThatThrownBy(() -> service.create(STRANGER, PET_ID, create(JUL_20)))
        .isInstanceOf(BusinessException.class)
        .extracting(e -> ((BusinessException) e).getErrorCode())
        .isEqualTo(ErrorCode.PET_FORBIDDEN);
  }

  @Test
  @DisplayName("[REQ-10-71] 삭제된 펫이면 가드의 PET_NOT_FOUND 가 그대로 나간다")
  void req_10_71_deletedPetGetsNotFoundFromGuard() {
    when(guard.getOwnedPet(PET_ID, OWNER))
        .thenThrow(new BusinessException(ErrorCode.PET_NOT_FOUND));

    assertThatThrownBy(() -> service.list(OWNER, PET_ID, new CursorRequest(null, 20)))
        .isInstanceOf(BusinessException.class)
        .extracting(e -> ((BusinessException) e).getErrorCode())
        .isEqualTo(ErrorCode.PET_NOT_FOUND);
  }

  // ── 기록 ↔ 펫 귀속 (D6) ─────────────────────────────────────

  @Test
  @DisplayName("[REQ-10-72] 다른 펫에 속한 기록 id 는 RESOURCE_NOT_FOUND 다")
  void req_10_72_recordOfAnotherPetIsNotFound() {
    owned(Species.CRESTED_GECKO);
    when(repository.findByIdAndPetId(RECORD_A, PET_ID)).thenReturn(Optional.empty());

    assertThatThrownBy(
            () ->
                service.update(
                    OWNER, PET_ID, RECORD_A, new ShedUpdateRequest(null, null, null, "m")))
        .isInstanceOf(BusinessException.class)
        .extracting(e -> ((BusinessException) e).getErrorCode())
        .isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);
  }

  // ── PATCH 병합 (D10) ────────────────────────────────────────

  @Test
  @DisplayName("[REQ-10-75] memo 만 보내면 is_complete 가 유지된다")
  void req_10_75_memoOnlyPatchKeepsIsComplete() {
    owned(Species.CRESTED_GECKO);
    ShedRecord entry = record(RECORD_A, JUL_20);
    when(repository.findByIdAndPetId(RECORD_A, PET_ID)).thenReturn(Optional.of(entry));

    service.update(OWNER, PET_ID, RECORD_A, new ShedUpdateRequest(null, null, null, "부분 탈피"));

    assertThat(entry.isComplete()).isTrue();
  }

  // ── keyset 커서 (D8) ────────────────────────────────────────

  @Test
  @DisplayName("[REQ-10-76] next_cursor 페이로드에 마지막 항목의 id 가 실린다")
  void req_10_76_nextCursorCarriesLastItemId() {
    owned(Species.CRESTED_GECKO);
    when(repository.findFirstPage(eq(PET_ID), any()))
        .thenReturn(
            List.of(record(RECORD_A, JUL_20), record(RECORD_B, JUL_20), record(RECORD_C, JUL_20)));

    CursorPage<ShedResponse> page = service.list(OWNER, PET_ID, new CursorRequest(null, 2));

    assertThat(codec.decode(page.nextCursor(), ShedCursor.class))
        .isEqualTo(new ShedCursor(JUL_20, RECORD_B));
  }

  @Test
  @DisplayName("[REQ-10-76] 다음 페이지 조회는 shed_date 와 id 를 둘 다 저장소에 넘긴다")
  void req_10_76_nextPagePassesBothKeysToRepository() {
    owned(Species.CRESTED_GECKO);
    when(repository.findPageAfter(any(), any(), any(), any())).thenReturn(List.of());

    service.list(
        OWNER, PET_ID, new CursorRequest(codec.encode(new ShedCursor(JUL_20, RECORD_B)), 2));

    verify(repository).findPageAfter(eq(PET_ID), eq(JUL_20), eq(RECORD_B), any());
  }

  // ── 종별 제한 (api-list.md § 8) ──────────────────────────────

  @Test
  @DisplayName("[REQ-10-78] 개 펫이 기록을 시도하면 FEATURE_NOT_SUPPORTED_SPECIES 다")
  void req_10_78_dogCreateIsRejected() {
    owned(Species.DOG);

    assertThatThrownBy(() -> service.create(OWNER, PET_ID, create(JUL_20)))
        .isInstanceOf(BusinessException.class)
        .extracting(e -> ((BusinessException) e).getErrorCode())
        .isEqualTo(ErrorCode.FEATURE_NOT_SUPPORTED_SPECIES);
  }

  @Test
  @DisplayName("[REQ-10-83] 게코 펫은 정상 저장된다")
  void req_10_83_geckoCreateSucceeds() {
    owned(Species.CRESTED_GECKO);

    assertThatCode(() -> service.create(OWNER, PET_ID, create(JUN_20))).doesNotThrowAnyException();
  }
}
