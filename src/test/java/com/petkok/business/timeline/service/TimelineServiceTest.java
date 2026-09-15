package com.petkok.business.timeline.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.petkok.business.pet.service.PetAccessGuard;
import com.petkok.data.activity.repository.ActivityLogRepository;
import com.petkok.data.diary.repository.DiaryEntryRepository;
import com.petkok.data.feeding.repository.FeedingLogRepository;
import com.petkok.data.pet.dto.OwnedPetResponse;
import com.petkok.data.pet.enums.Species;
import com.petkok.data.shed.repository.ShedRecordRepository;
import com.petkok.data.timeline.enums.TimelineType;
import com.petkok.data.weight.repository.WeightLogRepository;
import com.petkok.framework.constant.TimeConstant;
import com.petkok.framework.exception.BusinessException;
import com.petkok.framework.exception.ErrorCode;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * timeline 월간 집계의 <b>가드 위임 · KST 월 경계 변환</b>. 검증 계약 REQ-12-24 ~ 26 (PLAN-REQ-12 § 검증 계약).
 *
 * <p>병합·요약 로직 자체는 {@link TimelineMergerTest} 가 exhaustively 검증하므로 여기서는 중복하지 않는다 — 이 클래스는 {@link
 * PetAccessGuard} 위임(D5 재사용, {@code WeightServiceTest} REQ-10-06 과 동형)과 시각 경계 변환만 본다.
 *
 * <p>⚠️ 이 파일은 {@code TimelineService} 가 아직 없어 컴파일되지 않는다 — {@code /implement REQ-12 1} 이 만든다.
 *
 * <p><b>가정한 계약</b> — {@code TimelineService.list(UUID userId, UUID petId, YearMonth yearMonth,
 * TimelineType type)} 는 {@link PetAccessGuard#getOwnedPet} 으로 소유권을 확인한 뒤, 5개 리포지토리에 아직 없는 메서드를 새로
 * 추가해 그 달 기록을 조회한다(제작 단계에서 각 리포지토리 인터페이스에 추가 필요) —
 *
 * <ul>
 *   <li>{@code DiaryEntryRepository.findByPetIdAndEntryDateBetween(petId, from, to)}
 *   <li>{@code FeedingLogRepository.findByPetIdAndFedAtGreaterThanEqualAndFedAtLessThan(petId,
 *       from, toExclusive)}
 *   <li>{@code
 *       ActivityLogRepository.findByPetIdAndLoggedAtGreaterThanEqualAndLoggedAtLessThan(petId,
 *       from, toExclusive)}
 *   <li>{@code WeightLogRepository.findByPetIdAndMeasuredDateBetween(petId, from, to)}
 *   <li>{@code ShedRecordRepository.findByPetIdAndShedDateBetween(petId, from, to)}
 * </ul>
 *
 * <p>날짜 전용 도메인(diary·weight·shed)은 {@code LocalDate} 양 끝 포함(BETWEEN)이면 충분하지만, feeding·activity는
 * {@code timestamptz}라 월의 마지막 순간을 포함하려면 <b>다음 달 1일 KST 자정 미포함</b>으로 반열린 구간을 써야 한다(REQ-16/ADR-0002
 * "계산 = Asia/Seoul") — 이 경계 변환이 REQ-12-26 의 대상이다.
 */
class TimelineServiceTest {

  private static final UUID OWNER = UUID.fromString("11111111-1111-1111-1111-111111111111");
  private static final UUID STRANGER = UUID.fromString("22222222-2222-2222-2222-222222222222");
  private static final UUID PET_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
  private static final YearMonth JUNE_2026 = YearMonth.of(2026, 6);

  private final PetAccessGuard guard = mock(PetAccessGuard.class);
  private final DiaryEntryRepository diaryEntryRepository = mock(DiaryEntryRepository.class);
  private final FeedingLogRepository feedingLogRepository = mock(FeedingLogRepository.class);
  private final ActivityLogRepository activityLogRepository = mock(ActivityLogRepository.class);
  private final WeightLogRepository weightLogRepository = mock(WeightLogRepository.class);
  private final ShedRecordRepository shedRecordRepository = mock(ShedRecordRepository.class);

  private final TimelineService service =
      new TimelineService(
          guard,
          diaryEntryRepository,
          feedingLogRepository,
          activityLogRepository,
          weightLogRepository,
          shedRecordRepository);

  private void ownedByMe() {
    when(guard.getOwnedPet(PET_ID, OWNER))
        .thenReturn(new OwnedPetResponse(PET_ID, Species.CRESTED_GECKO));
    when(diaryEntryRepository.findByPetIdAndEntryDateBetween(any(), any(), any()))
        .thenReturn(List.of());
    when(weightLogRepository.findByPetIdAndMeasuredDateBetween(any(), any(), any()))
        .thenReturn(List.of());
    when(shedRecordRepository.findByPetIdAndShedDateBetween(any(), any(), any()))
        .thenReturn(List.of());
    when(feedingLogRepository.findByPetIdAndFedAtGreaterThanEqualAndFedAtLessThan(
            any(), any(), any()))
        .thenReturn(List.of());
    when(activityLogRepository.findByPetIdAndLoggedAtGreaterThanEqualAndLoggedAtLessThan(
            any(), any(), any()))
        .thenReturn(List.of());
  }

  @Test
  @DisplayName("[REQ-12-24] 남의 펫이면 가드의 PET_FORBIDDEN 이 그대로 나간다")
  void req_12_24_strangerGetsForbiddenFromGuard() {
    when(guard.getOwnedPet(PET_ID, STRANGER))
        .thenThrow(new BusinessException(ErrorCode.PET_FORBIDDEN));

    assertThatThrownBy(() -> service.list(STRANGER, PET_ID, JUNE_2026, TimelineType.ALL))
        .isInstanceOf(BusinessException.class)
        .extracting(e -> ((BusinessException) e).getErrorCode())
        .isEqualTo(ErrorCode.PET_FORBIDDEN);
  }

  @Test
  @DisplayName("[REQ-12-25] 없는 펫이면 가드의 PET_NOT_FOUND 가 그대로 나간다")
  void req_12_25_missingPetGetsNotFoundFromGuard() {
    when(guard.getOwnedPet(PET_ID, OWNER))
        .thenThrow(new BusinessException(ErrorCode.PET_NOT_FOUND));

    assertThatThrownBy(() -> service.list(OWNER, PET_ID, JUNE_2026, TimelineType.ALL))
        .isInstanceOf(BusinessException.class)
        .extracting(e -> ((BusinessException) e).getErrorCode())
        .isEqualTo(ErrorCode.PET_NOT_FOUND);
  }

  @Test
  @DisplayName(
      "[REQ-12-26] 2026-06 조회는 feeding·activity 를 KST 06-01T00:00+09:00 ~ 07-01T00:00+09:00(미포함) 로 조회한다")
  void req_12_26_feedingAndActivityQueriedWithKstMonthBoundary() {
    ownedByMe();
    OffsetDateTime expectedFrom =
        JUNE_2026.atDay(1).atStartOfDay(TimeConstant.KST).toOffsetDateTime();
    OffsetDateTime expectedToExclusive =
        JUNE_2026.plusMonths(1).atDay(1).atStartOfDay(TimeConstant.KST).toOffsetDateTime();

    service.list(OWNER, PET_ID, JUNE_2026, TimelineType.ALL);

    ArgumentCaptor<OffsetDateTime> feedingFrom = ArgumentCaptor.forClass(OffsetDateTime.class);
    ArgumentCaptor<OffsetDateTime> feedingTo = ArgumentCaptor.forClass(OffsetDateTime.class);
    verify(feedingLogRepository)
        .findByPetIdAndFedAtGreaterThanEqualAndFedAtLessThan(
            eq(PET_ID), feedingFrom.capture(), feedingTo.capture());
    assertThat(feedingFrom.getValue()).isEqualTo(expectedFrom);
    assertThat(feedingTo.getValue()).isEqualTo(expectedToExclusive);

    ArgumentCaptor<OffsetDateTime> activityFrom = ArgumentCaptor.forClass(OffsetDateTime.class);
    ArgumentCaptor<OffsetDateTime> activityTo = ArgumentCaptor.forClass(OffsetDateTime.class);
    verify(activityLogRepository)
        .findByPetIdAndLoggedAtGreaterThanEqualAndLoggedAtLessThan(
            eq(PET_ID), activityFrom.capture(), activityTo.capture());
    assertThat(activityFrom.getValue()).isEqualTo(expectedFrom);
    assertThat(activityTo.getValue()).isEqualTo(expectedToExclusive);
  }
}
