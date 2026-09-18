package com.petkok.business.timeline.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.petkok.data.activity.entity.ActivityLog;
import com.petkok.data.activity.enums.ActivityType;
import com.petkok.data.diary.entity.DiaryEntry;
import com.petkok.data.diary.enums.ConditionTag;
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
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * timeline 월간 집계의 <b>병합·필터·요약 규칙</b> — I/O 없는 순수 클래스. 검증 계약 REQ-12-01 ~ 23 (PLAN-REQ-12 § 검증 계약).
 *
 * <p>⚠️ 이 파일은 {@code TimelineMerger}·{@code TimelineType}·{@code TimelineDayResponse}·{@code
 * TimelineEventResponse} 가 아직 없어 컴파일되지 않는다 — {@code /implement REQ-12 1} 이 만든다.
 *
 * <p><b>가정한 계약</b> — {@code TimelineMerger.merge(List<DiaryEntry>, List<FeedingLog>,
 * List<ActivityLog>, List<WeightLog>, List<ShedRecord>, TimelineType)} 는 <b>순수 정적 메서드</b>다. 리포지토리·펫
 * 소유권은 모르고 이미 조회된 기록 목록만 받는다. 반환은 <b>기록이 있는 날짜만</b> 오름차순으로 묶은 {@code List<TimelineDayResponse>} —
 * 원본 예시가 기록 없는 날짜를 보여주지 않아 빈 날짜는 만들지 않는 쪽으로 가정했다(근거 없어 별도 케이스로 검증하지 않음). {@code
 * TimelineEventResponse} 는 공통 필드(type·refId·occurredAt·summary) + 도메인별 선택 필드
 * (conditionTag·isRefused·activityType·isComplete·isAssisted, 해당 없으면 {@code null})를 한 레코드에 담는다.
 */
class TimelineMergerTest {

  private static final UUID PET_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
  private static final LocalDate JUN_30 = LocalDate.of(2026, 6, 30);
  private static final LocalDate JUN_29 = LocalDate.of(2026, 6, 29);
  private static final OffsetDateTime JUN_30_MIDNIGHT_KST =
      JUN_30.atStartOfDay(TimeConstant.KST).toOffsetDateTime();

  private static UUID id(String suffix) {
    return UUID.fromString("aaaaaaaa-0000-0000-0000-" + suffix);
  }

  private static DiaryEntry diary(UUID entryId, LocalDate date, String title, String content) {
    DiaryEntry entry = DiaryEntry.of(PET_ID, title, content, ConditionTag.ACTIVE, date);
    ReflectionTestUtils.setField(entry, "id", entryId);
    return entry;
  }

  private static FeedingLog feeding(
      UUID logId,
      OffsetDateTime fedAt,
      String foodType,
      FoodSize foodSize,
      BigDecimal amount,
      String amountUnit,
      boolean isRefused) {
    FeedingLog log =
        FeedingLog.builder()
            .petId(PET_ID)
            .foodType(foodType)
            .foodSize(foodSize)
            .amount(amount)
            .amountUnit(amountUnit)
            .isRefused(isRefused)
            .fedAt(fedAt)
            .build();
    ReflectionTestUtils.setField(log, "id", logId);
    return log;
  }

  private static ActivityLog activity(
      UUID logId, OffsetDateTime loggedAt, ActivityType type, Integer durationMinutes) {
    ActivityLog log = ActivityLog.of(PET_ID, type, durationMinutes, null, null, loggedAt);
    ReflectionTestUtils.setField(log, "id", logId);
    return log;
  }

  private static WeightLog weight(UUID logId, LocalDate date, int weightG) {
    WeightLog log = WeightLog.of(PET_ID, weightG, date, null);
    ReflectionTestUtils.setField(log, "id", logId);
    return log;
  }

  private static ShedRecord shed(
      UUID recordId, LocalDate date, boolean complete, boolean assisted) {
    ShedRecord record = ShedRecord.of(PET_ID, date, complete, assisted, null);
    ReflectionTestUtils.setField(record, "id", recordId);
    return record;
  }

  private static TimelineDayResponse dayOf(List<TimelineDayResponse> days, LocalDate date) {
    return days.stream()
        .filter(d -> d.date().equals(date))
        .findFirst()
        .orElseThrow(() -> new AssertionError("날짜 " + date + " 가 결과에 없다: " + days));
  }

  // ── markers · type 필터 (REQ-12-01 ~ 03) ─────────────────────────────

  @Test
  @DisplayName("[REQ-12-01] 같은 날 여러 도메인 기록이 있으면 markers 에 그 타입이 모두 모인다")
  void req_12_01_markersCollectAllTypesOnSameDay() {
    List<TimelineDayResponse> days =
        TimelineMerger.merge(
            List.of(diary(id("000000000001"), JUN_30, "오늘의 두부", null)),
            List.of(
                feeding(
                    id("000000000002"),
                    JUN_30_MIDNIGHT_KST,
                    "귀뚜라미",
                    FoodSize.M,
                    null,
                    null,
                    false)),
            List.of(activity(id("000000000003"), JUN_30_MIDNIGHT_KST, ActivityType.HANDLING, 10)),
            List.of(weight(id("000000000004"), JUN_30, 62)),
            List.of(),
            TimelineType.ALL);

    assertThat(dayOf(days, JUN_30).markers())
        .containsExactlyInAnyOrder("diary", "feeding", "activity", "weight");
  }

  @Test
  @DisplayName("[REQ-12-02] type=weight 면 events 에는 weight 만 남는다")
  void req_12_02_typeFilterKeepsOnlyMatchingEvents() {
    List<TimelineDayResponse> days =
        TimelineMerger.merge(
            List.of(diary(id("000000000005"), JUN_30, "오늘의 두부", null)),
            List.of(),
            List.of(),
            List.of(weight(id("000000000006"), JUN_30, 62)),
            List.of(),
            TimelineType.WEIGHT);

    assertThat(dayOf(days, JUN_30).events())
        .extracting(TimelineEventResponse::type)
        .containsExactly("weight");
  }

  @Test
  @DisplayName("[REQ-12-03] type=weight 면 markers 도 weight 만 남는다")
  void req_12_03_typeFilterAlsoAppliesToMarkers() {
    List<TimelineDayResponse> days =
        TimelineMerger.merge(
            List.of(diary(id("000000000007"), JUN_30, "오늘의 두부", null)),
            List.of(),
            List.of(),
            List.of(weight(id("000000000008"), JUN_30, 62)),
            List.of(),
            TimelineType.WEIGHT);

    assertThat(dayOf(days, JUN_30).markers()).containsExactly("weight");
  }

  // ── events 정렬 · occurred_at (REQ-12-04 ~ 09) ────────────────────────

  @Test
  @DisplayName("[REQ-12-04] 하루 안 events 는 occurred_at 시간순으로 정렬된다")
  void req_12_04_eventsWithinDaySortedByOccurredAt() {
    OffsetDateTime morning = JUN_30_MIDNIGHT_KST.plusHours(9);
    OffsetDateTime evening = JUN_30_MIDNIGHT_KST.plusHours(18);

    List<TimelineDayResponse> days =
        TimelineMerger.merge(
            List.of(),
            List.of(feeding(id("000000000009"), evening, "귀뚜라미", FoodSize.M, null, null, false)),
            List.of(activity(id("00000000000a"), morning, ActivityType.HANDLING, 10)),
            List.of(),
            List.of(),
            TimelineType.ALL);

    assertThat(dayOf(days, JUN_30).events())
        .extracting(TimelineEventResponse::type)
        .containsExactly("activity", "feeding");
  }

  @Test
  @DisplayName("[REQ-12-05] diary occurred_at 은 KST 자정으로 고정된다")
  void req_12_05_diaryOccurredAtFixedToKstMidnight() {
    List<TimelineDayResponse> days =
        TimelineMerger.merge(
            List.of(diary(id("00000000000b"), JUN_30, "오늘의 두부", null)),
            List.of(),
            List.of(),
            List.of(),
            List.of(),
            TimelineType.ALL);

    assertThat(dayOf(days, JUN_30).events().get(0).occurredAt()).isEqualTo(JUN_30_MIDNIGHT_KST);
  }

  @Test
  @DisplayName("[REQ-12-06] weight occurred_at 도 KST 자정으로 고정된다")
  void req_12_06_weightOccurredAtFixedToKstMidnight() {
    List<TimelineDayResponse> days =
        TimelineMerger.merge(
            List.of(),
            List.of(),
            List.of(),
            List.of(weight(id("00000000000c"), JUN_30, 62)),
            List.of(),
            TimelineType.ALL);

    assertThat(dayOf(days, JUN_30).events().get(0).occurredAt()).isEqualTo(JUN_30_MIDNIGHT_KST);
  }

  @Test
  @DisplayName("[REQ-12-07] shed occurred_at 도 KST 자정으로 고정된다")
  void req_12_07_shedOccurredAtFixedToKstMidnight() {
    List<TimelineDayResponse> days =
        TimelineMerger.merge(
            List.of(),
            List.of(),
            List.of(),
            List.of(),
            List.of(shed(id("00000000000d"), JUN_30, true, false)),
            TimelineType.ALL);

    assertThat(dayOf(days, JUN_30).events().get(0).occurredAt()).isEqualTo(JUN_30_MIDNIGHT_KST);
  }

  @Test
  @DisplayName("[REQ-12-08] feeding occurred_at 은 fed_at 그대로다")
  void req_12_08_feedingOccurredAtIsFedAtAsIs() {
    OffsetDateTime fedAt = JUN_30_MIDNIGHT_KST.plusHours(18);

    List<TimelineDayResponse> days =
        TimelineMerger.merge(
            List.of(),
            List.of(feeding(id("00000000000e"), fedAt, "귀뚜라미", FoodSize.M, null, null, false)),
            List.of(),
            List.of(),
            List.of(),
            TimelineType.ALL);

    assertThat(dayOf(days, JUN_30).events().get(0).occurredAt()).isEqualTo(fedAt);
  }

  @Test
  @DisplayName("[REQ-12-09] activity occurred_at 은 logged_at 그대로다")
  void req_12_09_activityOccurredAtIsLoggedAtAsIs() {
    OffsetDateTime loggedAt = JUN_30_MIDNIGHT_KST.plusHours(9);

    List<TimelineDayResponse> days =
        TimelineMerger.merge(
            List.of(),
            List.of(),
            List.of(activity(id("00000000000f"), loggedAt, ActivityType.HANDLING, 10)),
            List.of(),
            List.of(),
            TimelineType.ALL);

    assertThat(dayOf(days, JUN_30).events().get(0).occurredAt()).isEqualTo(loggedAt);
  }

  // ── summary 생성 규칙 (REQ-12-10 ~ 19) ────────────────────────────────

  @Test
  @DisplayName("[REQ-12-10] diary summary — title 있으면 title 그대로")
  void req_12_10_diarySummaryUsesTitleWhenPresent() {
    List<TimelineDayResponse> days =
        TimelineMerger.merge(
            List.of(diary(id("000000000010"), JUN_30, "오늘의 두부", "장문의 본문 내용")),
            List.of(),
            List.of(),
            List.of(),
            List.of(),
            TimelineType.ALL);

    assertThat(dayOf(days, JUN_30).events().get(0).summary()).isEqualTo("오늘의 두부");
  }

  @Test
  @DisplayName("[REQ-12-11] diary summary — title 없고 content 있으면 앞 20자다")
  void req_12_11_diarySummaryFallsBackToContentPrefix() {
    String content = "가나다라마바사아자차카타파하가나다라마바사아자차카타파하";

    List<TimelineDayResponse> days =
        TimelineMerger.merge(
            List.of(diary(id("000000000011"), JUN_30, null, content)),
            List.of(),
            List.of(),
            List.of(),
            List.of(),
            TimelineType.ALL);

    assertThat(dayOf(days, JUN_30).events().get(0).summary()).isEqualTo(content.substring(0, 20));
  }

  @Test
  @DisplayName("[REQ-12-12] diary summary — title·content 둘 다 없으면 \"일지 기록\"이다")
  void req_12_12_diarySummaryDefaultsWhenBothMissing() {
    List<TimelineDayResponse> days =
        TimelineMerger.merge(
            List.of(diary(id("000000000012"), JUN_30, null, null)),
            List.of(),
            List.of(),
            List.of(),
            List.of(),
            TimelineType.ALL);

    assertThat(dayOf(days, JUN_30).events().get(0).summary()).isEqualTo("일지 기록");
  }

  @Test
  @DisplayName("[REQ-12-13] feeding summary — 원본 예시(\"귀뚜라미(M) 5마리\")를 재현한다")
  void req_12_13_feedingSummaryReproducesOriginalExample() {
    List<TimelineDayResponse> days =
        TimelineMerger.merge(
            List.of(),
            List.of(
                feeding(
                    id("000000000013"),
                    JUN_30_MIDNIGHT_KST,
                    "귀뚜라미",
                    FoodSize.M,
                    new BigDecimal("5"),
                    "마리",
                    false)),
            List.of(),
            List.of(),
            List.of(),
            TimelineType.ALL);

    assertThat(dayOf(days, JUN_30).events().get(0).summary()).isEqualTo("귀뚜라미(M) 5마리");
  }

  @Test
  @DisplayName("[REQ-12-34] feeding summary — foodSize 없으면 \"{foodType} {amount}{unit}\"이다")
  void req_12_34_feedingSummaryOmitsMissingFoodSize() {
    List<TimelineDayResponse> days =
        TimelineMerger.merge(
            List.of(),
            List.of(
                feeding(
                    id("000000000034"),
                    JUN_30_MIDNIGHT_KST,
                    "귀뚜라미",
                    null,
                    new BigDecimal("5"),
                    "마리",
                    false)),
            List.of(),
            List.of(),
            List.of(),
            TimelineType.ALL);

    assertThat(dayOf(days, JUN_30).events().get(0).summary()).isEqualTo("귀뚜라미 5마리");
  }

  @Test
  @DisplayName("[REQ-12-35] feeding summary — amount 없으면 \"{foodType}({foodSize})\"이다")
  void req_12_35_feedingSummaryOmitsMissingAmount() {
    List<TimelineDayResponse> days =
        TimelineMerger.merge(
            List.of(),
            List.of(
                feeding(
                    id("000000000035"),
                    JUN_30_MIDNIGHT_KST,
                    "귀뚜라미",
                    FoodSize.M,
                    null,
                    null,
                    false)),
            List.of(),
            List.of(),
            List.of(),
            TimelineType.ALL);

    assertThat(dayOf(days, JUN_30).events().get(0).summary()).isEqualTo("귀뚜라미(M)");
  }

  @Test
  @DisplayName("[REQ-12-36] feeding summary — foodType 없으면 \"({foodSize}) {amount}{unit}\"이다")
  void req_12_36_feedingSummaryOmitsMissingFoodType() {
    List<TimelineDayResponse> days =
        TimelineMerger.merge(
            List.of(),
            List.of(
                feeding(
                    id("000000000036"),
                    JUN_30_MIDNIGHT_KST,
                    null,
                    FoodSize.M,
                    new BigDecimal("5"),
                    "마리",
                    false)),
            List.of(),
            List.of(),
            List.of(),
            TimelineType.ALL);

    assertThat(dayOf(days, JUN_30).events().get(0).summary()).isEqualTo("(M) 5마리");
  }

  @Test
  @DisplayName("[REQ-12-37] feeding summary — 전 필드 누락이면 빈 문자열이다")
  void req_12_37_feedingSummaryIsEmptyWhenAllFieldsMissing() {
    List<TimelineDayResponse> days =
        TimelineMerger.merge(
            List.of(),
            List.of(
                feeding(id("000000000037"), JUN_30_MIDNIGHT_KST, null, null, null, null, false)),
            List.of(),
            List.of(),
            List.of(),
            TimelineType.ALL);

    assertThat(dayOf(days, JUN_30).events().get(0).summary()).isEmpty();
  }

  @Test
  @DisplayName("[REQ-12-14] weight summary — 원본 예시(\"62g\")를 재현한다")
  void req_12_14_weightSummaryReproducesOriginalExample() {
    List<TimelineDayResponse> days =
        TimelineMerger.merge(
            List.of(),
            List.of(),
            List.of(),
            List.of(weight(id("000000000014"), JUN_30, 62)),
            List.of(),
            TimelineType.ALL);

    assertThat(dayOf(days, JUN_30).events().get(0).summary()).isEqualTo("62g");
  }

  @Test
  @DisplayName("[REQ-12-15] activity summary — durationMinutes 있으면 \"{타입} {분}분\"이다")
  void req_12_15_activitySummaryIncludesDuration() {
    List<TimelineDayResponse> days =
        TimelineMerger.merge(
            List.of(),
            List.of(),
            List.of(activity(id("000000000015"), JUN_30_MIDNIGHT_KST, ActivityType.WALK, 30)),
            List.of(),
            List.of(),
            TimelineType.ALL);

    assertThat(dayOf(days, JUN_30).events().get(0).summary()).isEqualTo("WALK 30분");
  }

  @Test
  @DisplayName("[REQ-12-16] activity summary — durationMinutes 없으면 타입명만이다")
  void req_12_16_activitySummaryOmitsDurationWhenMissing() {
    List<TimelineDayResponse> days =
        TimelineMerger.merge(
            List.of(),
            List.of(),
            List.of(activity(id("000000000016"), JUN_30_MIDNIGHT_KST, ActivityType.WALK, null)),
            List.of(),
            List.of(),
            TimelineType.ALL);

    assertThat(dayOf(days, JUN_30).events().get(0).summary()).isEqualTo("WALK");
  }

  @Test
  @DisplayName("[REQ-12-17] shed summary — 완료·미도움은 \"탈피 완료\"다")
  void req_12_17_shedSummaryCompleteNotAssisted() {
    List<TimelineDayResponse> days =
        TimelineMerger.merge(
            List.of(),
            List.of(),
            List.of(),
            List.of(),
            List.of(shed(id("000000000017"), JUN_30, true, false)),
            TimelineType.ALL);

    assertThat(dayOf(days, JUN_30).events().get(0).summary()).isEqualTo("탈피 완료");
  }

  @Test
  @DisplayName("[REQ-12-18] shed summary — 미완료·도움은 \"탈피 도와줌\"이다")
  void req_12_18_shedSummaryNotCompleteAssisted() {
    List<TimelineDayResponse> days =
        TimelineMerger.merge(
            List.of(),
            List.of(),
            List.of(),
            List.of(),
            List.of(shed(id("000000000018"), JUN_30, false, true)),
            TimelineType.ALL);

    assertThat(dayOf(days, JUN_30).events().get(0).summary()).isEqualTo("탈피 도와줌");
  }

  @Test
  @DisplayName("[REQ-12-19] shed summary — 미완료·미도움은 \"탈피 진행 중\"이다")
  void req_12_19_shedSummaryNotCompleteNotAssisted() {
    List<TimelineDayResponse> days =
        TimelineMerger.merge(
            List.of(),
            List.of(),
            List.of(),
            List.of(),
            List.of(shed(id("000000000019"), JUN_30, false, false)),
            TimelineType.ALL);

    assertThat(dayOf(days, JUN_30).events().get(0).summary()).isEqualTo("탈피 진행 중");
  }

  @Test
  @DisplayName("[REQ-12-20] shed summary — 완료·도움(둘 다 true)은 is_assisted 가 우선해 \"탈피 도와줌\"이다")
  void req_12_20_shedSummaryCompleteAndAssistedPrioritizesAssisted() {
    List<TimelineDayResponse> days =
        TimelineMerger.merge(
            List.of(),
            List.of(),
            List.of(),
            List.of(),
            List.of(shed(id("00000000001a"), JUN_30, true, true)),
            TimelineType.ALL);

    assertThat(dayOf(days, JUN_30).events().get(0).summary()).isEqualTo("탈피 도와줌");
  }

  // ── 도메인별 선택 필드 (REQ-12-21 ~ 23) ────────────────────────────────

  @Test
  @DisplayName("[REQ-12-21] diary 이벤트에 condition_tag 가 실린다")
  void req_12_21_diaryEventIncludesConditionTag() {
    List<TimelineDayResponse> days =
        TimelineMerger.merge(
            List.of(diary(id("00000000001b"), JUN_30, "오늘의 두부", null)),
            List.of(),
            List.of(),
            List.of(),
            List.of(),
            TimelineType.ALL);

    assertThat(dayOf(days, JUN_30).events().get(0).conditionTag()).isEqualTo(ConditionTag.ACTIVE);
  }

  @Test
  @DisplayName("[REQ-12-22] feeding 이벤트에 is_refused 가 실린다")
  void req_12_22_feedingEventIncludesIsRefused() {
    List<TimelineDayResponse> days =
        TimelineMerger.merge(
            List.of(),
            List.of(
                feeding(
                    id("00000000001c"), JUN_30_MIDNIGHT_KST, "귀뚜라미", FoodSize.M, null, null, true)),
            List.of(),
            List.of(),
            List.of(),
            TimelineType.ALL);

    assertThat(dayOf(days, JUN_30).events().get(0).isRefused()).isTrue();
  }

  @Test
  @DisplayName("[REQ-12-23] weight 이벤트엔 도메인 선택 필드가 없다(summary만)")
  void req_12_23_weightEventHasNoDomainSpecificFields() {
    List<TimelineDayResponse> days =
        TimelineMerger.merge(
            List.of(),
            List.of(),
            List.of(),
            List.of(weight(id("00000000001d"), JUN_30, 62)),
            List.of(),
            TimelineType.ALL);

    TimelineEventResponse event = dayOf(days, JUN_30).events().get(0);
    assertThat(event.conditionTag()).isNull();
    assertThat(event.isRefused()).isNull();
    assertThat(event.activityType()).isNull();
    assertThat(event.isComplete()).isNull();
    assertThat(event.isAssisted()).isNull();
  }

  // ── 회귀 — 다른 날 기록은 섞이지 않는다 ─────────────────────────────────

  @Test
  @DisplayName("[REQ-12-01] 다른 날짜 기록은 서로 다른 day 로 분리된다")
  void req_12_01_recordsOnDifferentDatesStayInSeparateDays() {
    List<TimelineDayResponse> days =
        TimelineMerger.merge(
            List.of(),
            List.of(),
            List.of(),
            List.of(weight(id("00000000001e"), JUN_30, 62), weight(id("00000000001f"), JUN_29, 61)),
            List.of(),
            TimelineType.ALL);

    assertThat(dayOf(days, JUN_30).markers()).containsExactly("weight");
    assertThat(dayOf(days, JUN_29).markers()).containsExactly("weight");
  }
}
