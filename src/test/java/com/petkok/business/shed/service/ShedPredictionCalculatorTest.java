package com.petkok.business.shed.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.petkok.data.shed.dto.ShedPredictionResponse;
import com.petkok.data.shed.enums.PredictionConfidence;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 탈피 예측 계산기 — <b>I/O 없는 순수 클래스</b>(「소스 구조」 §1-4·§8). 검증 계약 REQ-10-84 ~ 91 (PLAN-REQ-10 § 검증 계약).
 *
 * <p>⚠️ 이 파일은 {@code ShedPredictionCalculator} 가 아직 없어 컴파일되지 않는다 — {@code /implement REQ-10 4} 가
 * 만든다.
 *
 * <p>가정한 계약 — {@code ShedPredictionCalculator.calculate(List<LocalDate> recentShedDatesDesc)} 는
 * {@code shed_date} 내림차순(최신이 먼저)으로 정렬된 리스트를 받는 순수 정적 메서드다. 기록 0건이면 전부 {@code null}/{@code 0} · 1건이면
 * 간격이 없어 {@code average_cycle_days}·{@code predicted_date} 가 {@code null}(사용자 확정 2026-09-02 — 성장
 * 단계별 주기 편차가 커 고정 기본값을 기각했다, {@code docs/reference/gecko-growth-and-shed-cycle.md} 참고). 2건 이상이면
 * <b>최근 3건까지만</b> 써서 간격 평균을 낸다.
 */
class ShedPredictionCalculatorTest {

  private static final LocalDate JAN_01 = LocalDate.of(2026, 1, 1);
  private static final LocalDate MAY_21 = LocalDate.of(2026, 5, 21);
  private static final LocalDate JUN_20 = LocalDate.of(2026, 6, 20);
  private static final LocalDate JUL_20 = LocalDate.of(2026, 7, 20);

  @Test
  @DisplayName(
      "[REQ-10-84] 기록 0건이면 predicted_date·average_cycle_days 가 null, based_on_records 는 0, confidence 는 LOW 다")
  void req_10_84_noRecordsReturnsAllNullLow() {
    ShedPredictionResponse response = ShedPredictionCalculator.calculate(List.of());

    assertThat(response)
        .isEqualTo(new ShedPredictionResponse(null, null, 0, PredictionConfidence.LOW));
  }

  @Test
  @DisplayName("[REQ-10-85] 기록 1건이면 confidence 는 LOW 다")
  void req_10_85_oneRecordIsLow() {
    ShedPredictionResponse response = ShedPredictionCalculator.calculate(List.of(JUL_20));

    assertThat(response.confidence()).isEqualTo(PredictionConfidence.LOW);
  }

  @Test
  @DisplayName("[REQ-10-85] 기록 1건이면 average_cycle_days 는 null 이다 (간격이 없다)")
  void req_10_85_oneRecordHasNullAverageCycleDays() {
    ShedPredictionResponse response = ShedPredictionCalculator.calculate(List.of(JUL_20));

    assertThat(response.averageCycleDays()).isNull();
  }

  @Test
  @DisplayName("[REQ-10-86] 기록 2건이면 confidence 는 MEDIUM 이다")
  void req_10_86_twoRecordsIsMedium() {
    ShedPredictionResponse response = ShedPredictionCalculator.calculate(List.of(JUL_20, JUN_20));

    assertThat(response.confidence()).isEqualTo(PredictionConfidence.MEDIUM);
  }

  @Test
  @DisplayName("[REQ-10-87] 기록 2건이면 average_cycle_days 는 두 기록 사이 일수다")
  void req_10_87_twoRecordsAverageIsTheSingleGap() {
    ShedPredictionResponse response = ShedPredictionCalculator.calculate(List.of(JUL_20, JUN_20));

    assertThat(response.averageCycleDays()).isEqualTo(30);
  }

  @Test
  @DisplayName("[REQ-10-88] 기록 2건이면 predicted_date 는 최근 shed_date + average_cycle_days 다")
  void req_10_88_twoRecordsPredictedDateIsMostRecentPlusAverage() {
    ShedPredictionResponse response = ShedPredictionCalculator.calculate(List.of(JUL_20, JUN_20));

    assertThat(response.predictedDate()).isEqualTo(LocalDate.of(2026, 8, 19));
  }

  @Test
  @DisplayName("[REQ-10-89] 기록 3건이면 confidence 는 HIGH 다")
  void req_10_89_threeRecordsIsHigh() {
    ShedPredictionResponse response =
        ShedPredictionCalculator.calculate(List.of(JUL_20, JUN_20, MAY_21));

    assertThat(response.confidence()).isEqualTo(PredictionConfidence.HIGH);
  }

  @Test
  @DisplayName("[REQ-10-90] 기록 3건이면 average_cycle_days 는 최근 2개 간격의 평균이다")
  void req_10_90_threeRecordsAverageIsTwoGapsAveraged() {
    ShedPredictionResponse response =
        ShedPredictionCalculator.calculate(List.of(JUL_20, JUN_20, MAY_21));

    assertThat(response.averageCycleDays()).isEqualTo(30);
  }

  @Test
  @DisplayName("[REQ-10-91] 기록 4건이면 가장 오래된 것은 무시하고 최근 3건만 쓴다 (based_on_records = 3)")
  void req_10_91_fourRecordsUsesOnlyMostRecentThree() {
    ShedPredictionResponse response =
        ShedPredictionCalculator.calculate(List.of(JUL_20, JUN_20, MAY_21, JAN_01));

    assertThat(response.basedOnRecords()).isEqualTo(3);
  }

  @Test
  @DisplayName("[REQ-10-91] 기록 4건이면 가장 오래된 기록은 간격 평균 계산에 들어가지 않는다")
  void req_10_91_fourRecordsIgnoresOldestGapInAverage() {
    // 4번째(JAN_01)가 계산에 들어갔다면 간격이 훨씬 커져 평균이 30일 수 없다.
    ShedPredictionResponse response =
        ShedPredictionCalculator.calculate(List.of(JUL_20, JUN_20, MAY_21, JAN_01));

    assertThat(response.averageCycleDays()).isEqualTo(30);
  }
}
