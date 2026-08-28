package com.petkok.business.activity.service;

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
import com.petkok.data.activity.dto.ActivityCreateRequest;
import com.petkok.data.activity.dto.ActivityResponse;
import com.petkok.data.activity.dto.ActivityUpdateRequest;
import com.petkok.data.activity.entity.ActivityLog;
import com.petkok.data.activity.enums.ActivityType;
import com.petkok.data.activity.repository.ActivityLogRepository;
import com.petkok.data.pet.dto.OwnedPetResponse;
import com.petkok.data.pet.enums.Species;
import com.petkok.framework.exception.BusinessException;
import com.petkok.framework.exception.ErrorCode;
import com.petkok.framework.pagination.CursorCodec;
import com.petkok.framework.pagination.CursorPage;
import com.petkok.framework.pagination.CursorRequest;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * 활동 기록의 <b>종별 검증 · 가드 위임 · D6 · 커서 · 병합</b>. 검증 계약 REQ-10-24 ~ 31 · 39 ~ 42 (PLAN-REQ-10 § 검증 계약).
 * {@code WeightServiceTest} 와 같은 구성 — 가드는 목, {@link CursorCodec} 은 실물.
 */
class ActivityServiceTest {

  private static final UUID OWNER = UUID.fromString("11111111-1111-1111-1111-111111111111");
  private static final UUID STRANGER = UUID.fromString("22222222-2222-2222-2222-222222222222");
  private static final UUID PET_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
  private static final UUID LOG_A = UUID.fromString("bbbbbbbb-0000-0000-0000-000000000003");
  private static final UUID LOG_B = UUID.fromString("bbbbbbbb-0000-0000-0000-000000000002");
  private static final UUID LOG_C = UUID.fromString("bbbbbbbb-0000-0000-0000-000000000001");
  private static final OffsetDateTime AT =
      OffsetDateTime.of(2026, 6, 30, 9, 0, 0, 0, ZoneOffset.UTC);

  private final PetAccessGuard guard = mock(PetAccessGuard.class);
  private final ActivityLogRepository repository = mock(ActivityLogRepository.class);
  private final CursorCodec codec = new CursorCodec(new ObjectMapper().findAndRegisterModules());
  private final ActivityService service = new ActivityService(guard, repository, codec);

  private static ActivityLog log(UUID id, ActivityType type, OffsetDateTime at) {
    ActivityLog log = ActivityLog.of(PET_ID, type, 30, null, null, at);
    ReflectionTestUtils.setField(log, "id", id);
    return log;
  }

  private static ActivityCreateRequest create(ActivityType type, BigDecimal distanceKm) {
    return new ActivityCreateRequest(type, 30, distanceKm, null, AT);
  }

  private void owned(Species species) {
    when(guard.getOwnedPet(PET_ID, OWNER)).thenReturn(new OwnedPetResponse(PET_ID, species));
    when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));
  }

  private void assertSpeciesViolation(Runnable call) {
    assertThatThrownBy(call::run)
        .isInstanceOf(BusinessException.class)
        .extracting(e -> ((BusinessException) e).getErrorCode())
        .isEqualTo(ErrorCode.INVALID_SPECIES_ACTIVITY);
  }

  // ── 종별 검증 ──────────────────────────────────────────────

  @Test
  @DisplayName("[REQ-10-24] 게코가 WALK 를 기록하면 INVALID_SPECIES_ACTIVITY")
  void req_10_24_geckoCannotWalk() {
    owned(Species.CRESTED_GECKO);
    assertSpeciesViolation(() -> service.create(OWNER, PET_ID, create(ActivityType.WALK, null)));
  }

  @Test
  @DisplayName("[REQ-10-25] 개가 HANDLING 을 기록하면 INVALID_SPECIES_ACTIVITY")
  void req_10_25_dogCannotHandle() {
    owned(Species.DOG);
    assertSpeciesViolation(
        () -> service.create(OWNER, PET_ID, create(ActivityType.HANDLING, null)));
  }

  @Test
  @DisplayName("[REQ-10-26] 고양이가 HANDLING 을 기록하면 INVALID_SPECIES_ACTIVITY")
  void req_10_26_catCannotHandle() {
    owned(Species.CAT);
    assertSpeciesViolation(
        () -> service.create(OWNER, PET_ID, create(ActivityType.HANDLING, null)));
  }

  @Test
  @DisplayName("[REQ-10-27] 개가 WALK 를 기록하면 저장된다")
  void req_10_27_dogCanWalk() {
    owned(Species.DOG);

    ActivityResponse response = service.create(OWNER, PET_ID, create(ActivityType.WALK, null));

    assertThat(response.activityType()).isEqualTo(ActivityType.WALK);
  }

  @Test
  @DisplayName("[REQ-10-28] 게코가 HANDLING 을 기록하면 저장된다")
  void req_10_28_geckoCanHandle() {
    owned(Species.CRESTED_GECKO);

    ActivityResponse response = service.create(OWNER, PET_ID, create(ActivityType.HANDLING, null));

    assertThat(response.activityType()).isEqualTo(ActivityType.HANDLING);
  }

  @Test
  @DisplayName("[REQ-10-29] PATCH 로 게코 기록의 유형을 WALK 로 바꾸면 종 검증이 다시 걸린다")
  void req_10_29_patchReappliesSpeciesRule() {
    owned(Species.CRESTED_GECKO);
    when(repository.findByIdAndPetId(LOG_A, PET_ID))
        .thenReturn(Optional.of(log(LOG_A, ActivityType.HANDLING, AT)));

    assertSpeciesViolation(
        () ->
            service.update(
                OWNER,
                PET_ID,
                LOG_A,
                new ActivityUpdateRequest(ActivityType.WALK, null, null, null, null)));
  }

  @Test
  @DisplayName("[REQ-10-30] PATCH 에 activity_type 이 없으면 종 검증 없이 통과한다")
  void req_10_30_patchWithoutTypeSkipsSpeciesRule() {
    owned(Species.CRESTED_GECKO);
    when(repository.findByIdAndPetId(LOG_A, PET_ID))
        .thenReturn(Optional.of(log(LOG_A, ActivityType.HANDLING, AT)));

    assertThatCode(
            () ->
                service.update(
                    OWNER, PET_ID, LOG_A, new ActivityUpdateRequest(null, null, null, "메모", null)))
        .doesNotThrowAnyException();
  }

  @Test
  @DisplayName("[REQ-10-31] 게코가 distance_km 를 보내면 거부하지 않고 그대로 저장한다")
  void req_10_31_geckoDistanceIsStoredAsIs() {
    owned(Species.CRESTED_GECKO);

    ActivityResponse response =
        service.create(OWNER, PET_ID, create(ActivityType.HANDLING, new BigDecimal("2.50")));

    assertThat(response.distanceKm()).isEqualByComparingTo("2.50");
  }

  // ── 병합 · 가드 · D6 · 커서 ──────────────────────────────

  @Test
  @DisplayName("[REQ-10-39] memo 만 보내면 duration_minutes 가 유지된다")
  void req_10_39_memoOnlyPatchKeepsDuration() {
    owned(Species.DOG);
    ActivityLog log = log(LOG_A, ActivityType.WALK, AT);
    when(repository.findByIdAndPetId(LOG_A, PET_ID)).thenReturn(Optional.of(log));

    service.update(OWNER, PET_ID, LOG_A, new ActivityUpdateRequest(null, null, null, "산책", null));

    assertThat(log.getDurationMinutes()).isEqualTo(30);
  }

  @Test
  @DisplayName("[REQ-10-40] 남의 펫이면 가드의 PET_FORBIDDEN 이 그대로 나간다")
  void req_10_40_strangerGetsForbiddenFromGuard() {
    when(guard.getOwnedPet(PET_ID, STRANGER))
        .thenThrow(new BusinessException(ErrorCode.PET_FORBIDDEN));

    assertThatThrownBy(() -> service.create(STRANGER, PET_ID, create(ActivityType.WALK, null)))
        .isInstanceOf(BusinessException.class)
        .extracting(e -> ((BusinessException) e).getErrorCode())
        .isEqualTo(ErrorCode.PET_FORBIDDEN);
  }

  @Test
  @DisplayName("[REQ-10-41] 다른 펫에 속한 기록 id 는 RESOURCE_NOT_FOUND 다")
  void req_10_41_recordOfAnotherPetIsNotFound() {
    owned(Species.DOG);
    when(repository.findByIdAndPetId(LOG_A, PET_ID)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.delete(OWNER, PET_ID, LOG_A))
        .isInstanceOf(BusinessException.class)
        .extracting(e -> ((BusinessException) e).getErrorCode())
        .isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);
  }

  @Test
  @DisplayName("[REQ-10-42] next_cursor 페이로드에 마지막 항목의 id 가 실린다")
  void req_10_42_nextCursorCarriesLastItemId() {
    owned(Species.DOG);
    when(repository.findFirstPage(eq(PET_ID), any()))
        .thenReturn(
            List.of(
                log(LOG_A, ActivityType.WALK, AT),
                log(LOG_B, ActivityType.WALK, AT),
                log(LOG_C, ActivityType.WALK, AT)));

    CursorPage<ActivityResponse> page = service.list(OWNER, PET_ID, new CursorRequest(null, 2));

    assertThat(codec.decode(page.nextCursor(), ActivityCursor.class))
        .isEqualTo(new ActivityCursor(AT, LOG_B));
  }

  @Test
  @DisplayName("[REQ-10-42] 다음 페이지 조회는 logged_at 과 id 를 둘 다 저장소에 넘긴다")
  void req_10_42_nextPagePassesBothKeysToRepository() {
    owned(Species.DOG);
    when(repository.findPageAfter(any(), any(), any(), any())).thenReturn(List.of());

    service.list(OWNER, PET_ID, new CursorRequest(codec.encode(new ActivityCursor(AT, LOG_B)), 2));

    verify(repository).findPageAfter(eq(PET_ID), eq(AT), eq(LOG_B), any());
  }
}
