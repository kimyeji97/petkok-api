package com.petkok.business.weight.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.petkok.business.pet.service.PetAccessGuard;
import com.petkok.data.common.entity.BaseSoftDeleteEntity;
import com.petkok.data.pet.dto.OwnedPetResponse;
import com.petkok.data.pet.enums.Species;
import com.petkok.data.weight.dto.WeightCreateRequest;
import com.petkok.data.weight.dto.WeightResponse;
import com.petkok.data.weight.dto.WeightUpdateRequest;
import com.petkok.data.weight.entity.WeightLog;
import com.petkok.data.weight.repository.WeightLogRepository;
import com.petkok.framework.exception.BusinessException;
import com.petkok.framework.exception.ErrorCode;
import com.petkok.framework.pagination.CursorCodec;
import com.petkok.framework.pagination.CursorPage;
import com.petkok.framework.pagination.CursorRequest;
import java.lang.reflect.Parameter;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * 체중 기록의 <b>가드 소비 · D6 · 커서 · 파생 필드</b>. 검증 계약 REQ-10-06 ~ 10 · 16 ~ 21 · 23 (PLAN-REQ-10 § 검증 계약).
 *
 * <p>{@link PetAccessGuard} 는 목이다 — 403 · 404 를 <b>가드가</b> 던지고 서비스는 그대로 흘리는 것(D5)이 검증 대상이라, 가드 안쪽(펫
 * 저장소)은 이 테스트의 관심이 아니다. {@link CursorCodec} 은 <b>실물</b>을 쓴다 — 커서 페이로드에 무엇이 실리는지가 REQ-10-10 의 검증
 * 대상이라 목으로 바꾸면 볼 것이 남지 않는다.
 *
 * <p>⚠️ <b>REQ-10-10 은 DB 없이 필요조건만 고정한다</b> — 이 레포에 DB 테스트 하네스가 없다. 실제 페이지 경계의 누락·중복은 Phase 1 완료 시
 * 로컬 DB 로 수동 확인한다(계획서 검증 계약 절 참고).
 */
class WeightServiceTest {

  private static final UUID OWNER = UUID.fromString("11111111-1111-1111-1111-111111111111");
  private static final UUID STRANGER = UUID.fromString("22222222-2222-2222-2222-222222222222");
  private static final UUID PET_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
  private static final UUID LOG_A = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000003");
  private static final UUID LOG_B = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000002");
  private static final UUID LOG_C = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000001");
  private static final LocalDate JUN_30 = LocalDate.of(2026, 6, 30);
  private static final LocalDate JUN_29 = LocalDate.of(2026, 6, 29);

  private final PetAccessGuard guard = mock(PetAccessGuard.class);
  private final WeightLogRepository repository = mock(WeightLogRepository.class);
  private final CursorCodec codec = new CursorCodec(new ObjectMapper().findAndRegisterModules());
  private final WeightService service = new WeightService(guard, repository, codec);

  private static WeightLog log(UUID id, int weightG, LocalDate measuredAt) {
    WeightLog log = WeightLog.of(PET_ID, weightG, measuredAt, null);
    ReflectionTestUtils.setField(log, "id", id);
    return log;
  }

  private void ownedByMe() {
    when(guard.getOwnedPet(PET_ID, OWNER))
        .thenReturn(new OwnedPetResponse(PET_ID, Species.CRESTED_GECKO));
    when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    when(repository.findPageAfter(any(), any(), any(), any())).thenReturn(List.of());
  }

  // ── 가드 위임 (D5) ──────────────────────────────────────────

  @Test
  @DisplayName("[REQ-10-06] 남의 펫이면 가드의 PET_FORBIDDEN 이 그대로 나간다")
  void req_10_06_strangerGetsForbiddenFromGuard() {
    when(guard.getOwnedPet(PET_ID, STRANGER))
        .thenThrow(new BusinessException(ErrorCode.PET_FORBIDDEN));

    assertThatThrownBy(
            () -> service.create(STRANGER, PET_ID, new WeightCreateRequest(62, JUN_30, null)))
        .isInstanceOf(BusinessException.class)
        .extracting(e -> ((BusinessException) e).getErrorCode())
        .isEqualTo(ErrorCode.PET_FORBIDDEN);
  }

  @Test
  @DisplayName("[REQ-10-07] 삭제된 펫이면 가드의 PET_NOT_FOUND 가 그대로 나간다")
  void req_10_07_deletedPetGetsNotFoundFromGuard() {
    when(guard.getOwnedPet(PET_ID, OWNER))
        .thenThrow(new BusinessException(ErrorCode.PET_NOT_FOUND));

    assertThatThrownBy(() -> service.list(OWNER, PET_ID, new CursorRequest(null, 20)))
        .isInstanceOf(BusinessException.class)
        .extracting(e -> ((BusinessException) e).getErrorCode())
        .isEqualTo(ErrorCode.PET_NOT_FOUND);
  }

  // ── 기록 ↔ 펫 귀속 (D6) ─────────────────────────────────────

  @Test
  @DisplayName("[REQ-10-08] 다른 펫에 속한 기록 id 는 RESOURCE_NOT_FOUND 다")
  void req_10_08_recordOfAnotherPetIsNotFound() {
    ownedByMe();
    when(repository.findByIdAndPetId(LOG_A, PET_ID)).thenReturn(Optional.empty());

    assertThatThrownBy(
            () -> service.update(OWNER, PET_ID, LOG_A, new WeightUpdateRequest(null, null, "m")))
        .isInstanceOf(BusinessException.class)
        .extracting(e -> ((BusinessException) e).getErrorCode())
        .isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);
  }

  @Test
  @DisplayName("[REQ-10-08] 기록 조회에 pet_id 를 함께 건다 — findById 단독 호출이 없다")
  void req_10_08_neverLooksUpByIdAlone() {
    ownedByMe();
    when(repository.findByIdAndPetId(LOG_A, PET_ID))
        .thenReturn(Optional.of(log(LOG_A, 50, JUN_30)));

    service.delete(OWNER, PET_ID, LOG_A);

    verify(repository, never()).findById(any());
  }

  // ── 가드 소비 형태 (D5) ─────────────────────────────────────

  @Test
  @DisplayName("[REQ-10-09] WeightService 생성자는 PetRepository · Pet 을 받지 않는다")
  void req_10_09_constructorDoesNotTakePetRepositoryOrEntity() {
    List<String> paramTypes =
        Arrays.stream(WeightService.class.getConstructors())
            .flatMap(c -> Arrays.stream(c.getParameters()))
            .map(Parameter::getType)
            .map(Class::getName)
            .toList();

    assertThat(paramTypes)
        .doesNotContain(
            "com.petkok.data.pet.repository.PetRepository", "com.petkok.data.pet.entity.Pet");
  }

  // ── keyset 커서 (D8) ────────────────────────────────────────

  @Test
  @DisplayName("[REQ-10-10] next_cursor 페이로드에 마지막 항목의 id 가 실린다 (타이브레이크)")
  void req_10_10_nextCursorCarriesLastItemId() {
    ownedByMe();
    // limit 2 인데 같은 날짜 3건 — 저장소는 limit+1 을 돌려준다
    when(repository.findFirstPage(eq(PET_ID), any()))
        .thenReturn(
            List.of(log(LOG_A, 50, JUN_30), log(LOG_B, 50, JUN_30), log(LOG_C, 50, JUN_30)));

    CursorPage<WeightResponse> page = service.list(OWNER, PET_ID, new CursorRequest(null, 2));

    WeightCursor cursor = codec.decode(page.nextCursor(), WeightCursor.class);
    assertThat(cursor).isEqualTo(new WeightCursor(JUN_30, LOG_B));
  }

  @Test
  @DisplayName("[REQ-10-10] 다음 페이지 조회는 measured_at 과 id 를 둘 다 저장소에 넘긴다")
  void req_10_10_nextPagePassesBothKeysToRepository() {
    ownedByMe();
    String cursor = codec.encode(new WeightCursor(JUN_30, LOG_B));

    service.list(OWNER, PET_ID, new CursorRequest(cursor, 2));

    verify(repository).findPageAfter(eq(PET_ID), eq(JUN_30), eq(LOG_B), any());
  }

  @Test
  @DisplayName("[REQ-10-10] limit 만큼만 돌려주고 has_next 가 켜진다")
  void req_10_10_returnsLimitItemsAndHasNext() {
    ownedByMe();
    when(repository.findFirstPage(eq(PET_ID), any()))
        .thenReturn(
            List.of(log(LOG_A, 50, JUN_30), log(LOG_B, 50, JUN_30), log(LOG_C, 50, JUN_30)));

    CursorPage<WeightResponse> page = service.list(OWNER, PET_ID, new CursorRequest(null, 2));

    assertThat(page.items()).hasSize(2);
    assertThat(page.hasNext()).isTrue();
  }

  @Test
  @DisplayName("[REQ-10-23] 해석 불가한 cursor 는 INVALID_CURSOR 다")
  void req_10_23_garbageCursorIsRejected() {
    ownedByMe();

    assertThatThrownBy(() -> service.list(OWNER, PET_ID, new CursorRequest("!!not-a-cursor!!", 20)))
        .isInstanceOf(BusinessException.class)
        .extracting(e -> ((BusinessException) e).getErrorCode())
        .isEqualTo(ErrorCode.INVALID_CURSOR);
  }

  // ── PATCH 병합 (D10) ────────────────────────────────────────

  @Test
  @DisplayName("[REQ-10-16] memo 만 보내면 weight_g 가 유지된다")
  void req_10_16_memoOnlyPatchKeepsWeight() {
    ownedByMe();
    WeightLog log = log(LOG_A, 50, JUN_30);
    when(repository.findByIdAndPetId(LOG_A, PET_ID)).thenReturn(Optional.of(log));

    service.update(OWNER, PET_ID, LOG_A, new WeightUpdateRequest(null, null, "아침"));

    assertThat(log.getWeightG()).isEqualTo(50);
  }

  @Test
  @DisplayName("[REQ-10-16] memo 만 보내면 measured_at 도 유지된다")
  void req_10_16_memoOnlyPatchKeepsMeasuredAt() {
    ownedByMe();
    WeightLog log = log(LOG_A, 50, JUN_30);
    when(repository.findByIdAndPetId(LOG_A, PET_ID)).thenReturn(Optional.of(log));

    service.update(OWNER, PET_ID, LOG_A, new WeightUpdateRequest(null, null, "아침"));

    assertThat(log.getMeasuredAt()).isEqualTo(JUN_30);
  }

  // ── 하드 삭제 (D7) ──────────────────────────────────────────

  @Test
  @DisplayName("[REQ-10-17] 삭제는 저장소의 행 삭제다")
  void req_10_17_deleteRemovesRow() {
    ownedByMe();
    WeightLog log = log(LOG_A, 50, JUN_30);
    when(repository.findByIdAndPetId(LOG_A, PET_ID)).thenReturn(Optional.of(log));

    service.delete(OWNER, PET_ID, LOG_A);

    verify(repository).delete(log);
  }

  @Test
  @DisplayName("[REQ-10-17] WeightLog 는 소프트 딜리트 엔티티가 아니다")
  void req_10_17_entityHasNoSoftDelete() {
    assertThat(BaseSoftDeleteEntity.class.isAssignableFrom(WeightLog.class)).isFalse();
  }

  // ── 체중 경고 파생 필드 (D3) ────────────────────────────────

  @Test
  @DisplayName("[REQ-10-18] 첫 기록은 weight_change_rate 가 null 이다")
  void req_10_18_firstRecordHasNullRate() {
    ownedByMe();

    WeightResponse response =
        service.create(OWNER, PET_ID, new WeightCreateRequest(62, JUN_30, null));

    assertThat(response.weightChangeRate()).isNull();
  }

  @Test
  @DisplayName("[REQ-10-18] 첫 기록은 is_weight_warning 이 false 다")
  void req_10_18_firstRecordHasNoWarning() {
    ownedByMe();

    WeightResponse response =
        service.create(OWNER, PET_ID, new WeightCreateRequest(62, JUN_30, null));

    assertThat(response.isWeightWarning()).isFalse();
  }

  @Test
  @DisplayName("[REQ-10-19] 직전 50g → 60g 이면 weight_change_rate 가 20.0 이다")
  void req_10_19_exactTwentyPercentRate() {
    ownedByMe();
    when(repository.findPageAfter(eq(PET_ID), eq(JUN_30), any(), any()))
        .thenReturn(List.of(log(LOG_B, 50, JUN_29)));

    WeightResponse response =
        service.create(OWNER, PET_ID, new WeightCreateRequest(60, JUN_30, null));

    assertThat(response.weightChangeRate()).isEqualTo(20.0);
  }

  @Test
  @DisplayName("[REQ-10-19] 정확히 20% 는 경고에 포함된다")
  void req_10_19_exactTwentyPercentIsWarning() {
    ownedByMe();
    when(repository.findPageAfter(eq(PET_ID), eq(JUN_30), any(), any()))
        .thenReturn(List.of(log(LOG_B, 50, JUN_29)));

    WeightResponse response =
        service.create(OWNER, PET_ID, new WeightCreateRequest(60, JUN_30, null));

    assertThat(response.isWeightWarning()).isTrue();
  }

  @Test
  @DisplayName("[REQ-10-20] 직전 50g → 59g 이면 weight_change_rate 가 18.0 이다")
  void req_10_20_eighteenPercentRate() {
    ownedByMe();
    when(repository.findPageAfter(eq(PET_ID), eq(JUN_30), any(), any()))
        .thenReturn(List.of(log(LOG_B, 50, JUN_29)));

    WeightResponse response =
        service.create(OWNER, PET_ID, new WeightCreateRequest(59, JUN_30, null));

    assertThat(response.weightChangeRate()).isEqualTo(18.0);
  }

  @Test
  @DisplayName("[REQ-10-20] 18% 는 경고가 아니다")
  void req_10_20_eighteenPercentIsNotWarning() {
    ownedByMe();
    when(repository.findPageAfter(eq(PET_ID), eq(JUN_30), any(), any()))
        .thenReturn(List.of(log(LOG_B, 50, JUN_29)));

    WeightResponse response =
        service.create(OWNER, PET_ID, new WeightCreateRequest(59, JUN_30, null));

    assertThat(response.isWeightWarning()).isFalse();
  }

  @Test
  @DisplayName("[REQ-10-21] 목록에서 직전은 바로 다음 항목이다 — 그 다음 항목이 아니다")
  void req_10_21_previousIsImmediateNextInList() {
    ownedByMe();
    // 60 → 직전 50 (+20%) 이어야지, 그 다음 100 (-40%) 이면 안 된다
    when(repository.findFirstPage(eq(PET_ID), any()))
        .thenReturn(
            List.of(log(LOG_A, 60, JUN_30), log(LOG_B, 50, JUN_29), log(LOG_C, 100, JUN_29)));

    CursorPage<WeightResponse> page = service.list(OWNER, PET_ID, new CursorRequest(null, 20));

    assertThat(page.items().get(0).weightChangeRate()).isEqualTo(20.0);
  }

  @Test
  @DisplayName("[REQ-10-21] 목록의 마지막 항목은 페이지 밖의 직전 1건과 비교한다")
  void req_10_21_lastItemComparesWithRecordBeyondPage() {
    ownedByMe();
    when(repository.findFirstPage(eq(PET_ID), any())).thenReturn(List.of(log(LOG_A, 60, JUN_30)));
    when(repository.findPageAfter(eq(PET_ID), eq(JUN_30), eq(LOG_A), any()))
        .thenReturn(List.of(log(LOG_B, 50, JUN_29)));

    CursorPage<WeightResponse> page = service.list(OWNER, PET_ID, new CursorRequest(null, 20));

    assertThat(page.items().get(0).weightChangeRate()).isEqualTo(20.0);
  }
}
