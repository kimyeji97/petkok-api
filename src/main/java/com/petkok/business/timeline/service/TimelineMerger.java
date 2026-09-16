package com.petkok.business.timeline.service;

import com.petkok.data.activity.entity.ActivityLog;
import com.petkok.data.activity.enums.ActivityType;
import com.petkok.data.diary.entity.DiaryEntry;
import com.petkok.data.feeding.entity.FeedingLog;
import com.petkok.data.feeding.enums.FoodSize;
import com.petkok.data.shed.entity.ShedRecord;
import com.petkok.data.timeline.dto.TimelineDayResponse;
import com.petkok.data.timeline.dto.TimelineEventResponse;
import com.petkok.data.timeline.enums.TimelineType;
import com.petkok.data.weight.entity.WeightLog;
import com.petkok.framework.constant.TimeConstant;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * timeline 월간 집계의 병합·필터·요약 규칙 — I/O 없는 순수 정적 클래스 (AGENTS §3 "*Calculator" 계통). 검증 계약 REQ-12-01 ~
 * 23.
 *
 * <p>이미 조회된 도메인별 기록 목록을 받아 날짜별로 묶는다. {@code type} 필터는 markers·events 양쪽에 함께 적용된다(PLAN-REQ-12 §결정
 * "markers도 type으로 필터링").
 */
public final class TimelineMerger {

  private static final int DIARY_SUMMARY_PREFIX_LENGTH = 20;

  private TimelineMerger() {}

  public static List<TimelineDayResponse> merge(
      List<DiaryEntry> diaryEntries,
      List<FeedingLog> feedingLogs,
      List<ActivityLog> activityLogs,
      List<WeightLog> weightLogs,
      List<ShedRecord> shedRecords,
      TimelineType type) {

    Map<LocalDate, List<TimelineEventResponse>> eventsByDate = new TreeMap<>();

    if (matches(type, TimelineType.DIARY)) {
      for (DiaryEntry entry : diaryEntries) {
        add(eventsByDate, entry.getEntryDate(), toEvent(entry));
      }
    }
    if (matches(type, TimelineType.FEEDING)) {
      for (FeedingLog logEntry : feedingLogs) {
        add(eventsByDate, toKstDate(logEntry.getFedAt()), toEvent(logEntry));
      }
    }
    if (matches(type, TimelineType.ACTIVITY)) {
      for (ActivityLog logEntry : activityLogs) {
        add(eventsByDate, toKstDate(logEntry.getLoggedAt()), toEvent(logEntry));
      }
    }
    if (matches(type, TimelineType.WEIGHT)) {
      for (WeightLog logEntry : weightLogs) {
        add(eventsByDate, logEntry.getMeasuredDate(), toEvent(logEntry));
      }
    }
    if (matches(type, TimelineType.SHED)) {
      for (ShedRecord record : shedRecords) {
        add(eventsByDate, record.getShedDate(), toEvent(record));
      }
    }

    List<TimelineDayResponse> days = new ArrayList<>();
    for (Map.Entry<LocalDate, List<TimelineEventResponse>> entry : eventsByDate.entrySet()) {
      List<TimelineEventResponse> sorted =
          entry.getValue().stream()
              .sorted(Comparator.comparing(TimelineEventResponse::occurredAt))
              .toList();
      List<String> markers = sorted.stream().map(TimelineEventResponse::type).distinct().toList();
      days.add(new TimelineDayResponse(entry.getKey(), markers, sorted));
    }
    return days;
  }

  private static boolean matches(TimelineType filter, TimelineType candidate) {
    return filter == TimelineType.ALL || filter == candidate;
  }

  private static void add(
      Map<LocalDate, List<TimelineEventResponse>> eventsByDate,
      LocalDate date,
      TimelineEventResponse event) {
    eventsByDate.computeIfAbsent(date, d -> new ArrayList<>()).add(event);
  }

  private static LocalDate toKstDate(OffsetDateTime instant) {
    return instant.atZoneSameInstant(TimeConstant.KST).toLocalDate();
  }

  private static OffsetDateTime kstMidnight(LocalDate date) {
    return date.atStartOfDay(TimeConstant.KST).toOffsetDateTime();
  }

  private static TimelineEventResponse toEvent(DiaryEntry entry) {
    return new TimelineEventResponse(
        "diary",
        entry.getId(),
        kstMidnight(entry.getEntryDate()),
        diarySummary(entry.getTitle(), entry.getContent()),
        entry.getConditionTag(),
        null,
        null,
        null,
        null);
  }

  private static String diarySummary(String title, String content) {
    if (title != null) {
      return title;
    }
    if (content != null) {
      return content.length() > DIARY_SUMMARY_PREFIX_LENGTH
          ? content.substring(0, DIARY_SUMMARY_PREFIX_LENGTH)
          : content;
    }
    return "일지 기록";
  }

  private static TimelineEventResponse toEvent(FeedingLog logEntry) {
    return new TimelineEventResponse(
        "feeding",
        logEntry.getId(),
        logEntry.getFedAt(),
        feedingSummary(
            logEntry.getFoodType(),
            logEntry.getFoodSize(),
            logEntry.getAmount(),
            logEntry.getAmountUnit()),
        null,
        logEntry.isRefused(),
        null,
        null,
        null);
  }

  private static String feedingSummary(
      String foodType, FoodSize foodSize, BigDecimal amount, String amountUnit) {
    StringBuilder summary = new StringBuilder();
    if (foodType != null) {
      summary.append(foodType);
    }
    if (foodSize != null) {
      summary.append('(').append(foodSize.name()).append(')');
    }
    if (amount != null) {
      if (!summary.isEmpty()) {
        summary.append(' ');
      }
      summary.append(amount.stripTrailingZeros().toPlainString());
      if (amountUnit != null) {
        summary.append(amountUnit);
      }
    }
    return summary.toString();
  }

  private static TimelineEventResponse toEvent(ActivityLog logEntry) {
    return new TimelineEventResponse(
        "activity",
        logEntry.getId(),
        logEntry.getLoggedAt(),
        activitySummary(logEntry.getActivityType(), logEntry.getDurationMinutes()),
        null,
        null,
        logEntry.getActivityType(),
        null,
        null);
  }

  private static String activitySummary(ActivityType activityType, Integer durationMinutes) {
    return durationMinutes != null
        ? activityType.name() + " " + durationMinutes + "분"
        : activityType.name();
  }

  private static TimelineEventResponse toEvent(WeightLog logEntry) {
    return new TimelineEventResponse(
        "weight",
        logEntry.getId(),
        kstMidnight(logEntry.getMeasuredDate()),
        logEntry.getWeightG() + "g",
        null,
        null,
        null,
        null,
        null);
  }

  private static TimelineEventResponse toEvent(ShedRecord record) {
    return new TimelineEventResponse(
        "shed",
        record.getId(),
        kstMidnight(record.getShedDate()),
        shedSummary(record.isComplete(), record.isAssisted()),
        null,
        null,
        null,
        record.isComplete(),
        record.isAssisted());
  }

  /** {@code is_assisted} 가 우선한다 — 이미 "탈피도와줌" 상태의 단일 출처다(ADR-0001, PROGRESS.md 2026-09-03). */
  private static String shedSummary(boolean complete, boolean assisted) {
    if (assisted) {
      return "탈피 도와줌";
    }
    return complete ? "탈피 완료" : "탈피 진행 중";
  }
}
