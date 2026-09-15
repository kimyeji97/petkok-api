package com.petkok.business.timeline.service;

import com.petkok.business.pet.service.PetAccessGuard;
import com.petkok.data.activity.entity.ActivityLog;
import com.petkok.data.activity.repository.ActivityLogRepository;
import com.petkok.data.diary.entity.DiaryEntry;
import com.petkok.data.diary.repository.DiaryEntryRepository;
import com.petkok.data.feeding.entity.FeedingLog;
import com.petkok.data.feeding.repository.FeedingLogRepository;
import com.petkok.data.pet.dto.OwnedPetResponse;
import com.petkok.data.shed.entity.ShedRecord;
import com.petkok.data.shed.repository.ShedRecordRepository;
import com.petkok.data.timeline.dto.TimelineDayResponse;
import com.petkok.data.timeline.dto.TimelineResponse;
import com.petkok.data.timeline.enums.TimelineType;
import com.petkok.data.weight.entity.WeightLog;
import com.petkok.data.weight.repository.WeightLogRepository;
import com.petkok.framework.constant.TimeConstant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * timeline 월간 집계 — 5개 도메인(diary·feeding·activity·weight·shed) 리포지토리를 앱 레벨에서 조합한다(PLAN-REQ-12 §결정 "앱
 * 레벨 병합(옵션 A)"). 검증 계약 REQ-12-24 ~ 26.
 *
 * <p>진입 = {@link PetAccessGuard}(D5, 다른 기록 도메인과 동일 패턴). 병합·요약 로직 자체는 {@link TimelineMerger}(순수 정적)에
 * 위임한다.
 */
@Service
public class TimelineService {

  private final PetAccessGuard petAccessGuard;
  private final DiaryEntryRepository diaryEntryRepository;
  private final FeedingLogRepository feedingLogRepository;
  private final ActivityLogRepository activityLogRepository;
  private final WeightLogRepository weightLogRepository;
  private final ShedRecordRepository shedRecordRepository;

  public TimelineService(
      PetAccessGuard petAccessGuard,
      DiaryEntryRepository diaryEntryRepository,
      FeedingLogRepository feedingLogRepository,
      ActivityLogRepository activityLogRepository,
      WeightLogRepository weightLogRepository,
      ShedRecordRepository shedRecordRepository) {
    this.petAccessGuard = petAccessGuard;
    this.diaryEntryRepository = diaryEntryRepository;
    this.feedingLogRepository = feedingLogRepository;
    this.activityLogRepository = activityLogRepository;
    this.weightLogRepository = weightLogRepository;
    this.shedRecordRepository = shedRecordRepository;
  }

  /**
   * 검증 계약 REQ-12-24 ~ 26. 날짜 전용 도메인(diary·weight·shed)은 {@code LocalDate} 양 끝 포함, feeding·activity는
   * KST 월 경계 반열린 구간(ADR-0002 "계산은 KST 기준") — REQ-12-26 대상.
   */
  @Transactional(readOnly = true)
  public TimelineResponse list(UUID userId, UUID petId, YearMonth yearMonth, TimelineType type) {
    OwnedPetResponse pet = petAccessGuard.getOwnedPet(petId, userId);

    LocalDate monthStart = yearMonth.atDay(1);
    LocalDate monthEnd = yearMonth.atEndOfMonth();
    OffsetDateTime kstFrom = monthStart.atStartOfDay(TimeConstant.KST).toOffsetDateTime();
    OffsetDateTime kstToExclusive =
        yearMonth.plusMonths(1).atDay(1).atStartOfDay(TimeConstant.KST).toOffsetDateTime();

    List<DiaryEntry> diaryEntries =
        diaryEntryRepository.findByPetIdAndEntryDateBetween(pet.id(), monthStart, monthEnd);
    List<FeedingLog> feedingLogs =
        feedingLogRepository.findByPetIdAndFedAtGreaterThanEqualAndFedAtLessThan(
            pet.id(), kstFrom, kstToExclusive);
    List<ActivityLog> activityLogs =
        activityLogRepository.findByPetIdAndLoggedAtGreaterThanEqualAndLoggedAtLessThan(
            pet.id(), kstFrom, kstToExclusive);
    List<WeightLog> weightLogs =
        weightLogRepository.findByPetIdAndMeasuredDateBetween(pet.id(), monthStart, monthEnd);
    List<ShedRecord> shedRecords =
        shedRecordRepository.findByPetIdAndShedDateBetween(pet.id(), monthStart, monthEnd);

    List<TimelineDayResponse> days =
        TimelineMerger.merge(
            diaryEntries, feedingLogs, activityLogs, weightLogs, shedRecords, type);
    return new TimelineResponse(days);
  }
}
