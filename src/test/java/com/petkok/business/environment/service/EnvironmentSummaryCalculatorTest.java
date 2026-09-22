package com.petkok.business.environment.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.petkok.data.environment.dto.EnvironmentDailySummaryResponse;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 일간 온습도 평균 계산기 — <b>I/O 없는 순수 클래스</b>(「소스 구조」 §1-4·§8, {@code ShedPredictionCalculator}·{@code
 * AnorexiaStreakCalculator}와 같은 패턴). 검증 계약 REQ-20-09 ~ 13 (PLAN-REQ-20 § 검증 계약).
 *
 * <p>⚠️ 이 파일은 {@code EnvironmentSummaryCalculator} 가 아직 없어 컴파일되지 않는다 — {@code /implement REQ-20 1}
 * 이 만든다.
 *
 * <p>가정한 계약 — {@code EnvironmentSummaryCalculator.calculate(List<BigDecimal> temperatures,
 * List<BigDecimal> humidities)} 는 같은 날 기록들의 온도·습도를 각각 산술 평균 내는 순수 정적 메서드다. 저장하지 않고 조회 시
 * 계산한다(PLAN-REQ-20 §제약·함정). 두 리스트는 같은 길이이고 같은 인덱스가 한 기록의 온도·습도 쌍이라고 가정한다.
 */
class EnvironmentSummaryCalculatorTest {

  @Test
  @DisplayName("[REQ-20-09] 기록 0건이면 avg_temperature·avg_humidity 가 null, record_count 는 0이다")
  void req_20_09_noRecordsReturnsNullAveragesAndZeroCount() {
    EnvironmentDailySummaryResponse response =
        EnvironmentSummaryCalculator.calculate(List.of(), List.of());

    assertThat(response).isEqualTo(new EnvironmentDailySummaryResponse(null, null, 0));
  }

  @Test
  @DisplayName("[REQ-20-10] 기록 1건이면 avg_temperature 가 그 기록의 값과 같다")
  void req_20_10_singleRecordAverageTemperatureEqualsItsValue() {
    EnvironmentDailySummaryResponse response =
        EnvironmentSummaryCalculator.calculate(
            List.of(new BigDecimal("24.5")), List.of(new BigDecimal("65.0")));

    assertThat(response.avgTemperature()).isEqualByComparingTo("24.5");
  }

  @Test
  @DisplayName("[REQ-20-11] 기록 여러 건이면 avg_temperature 가 산술 평균이다")
  void req_20_11_multipleRecordsAverageTemperatureIsArithmeticMean() {
    EnvironmentDailySummaryResponse response =
        EnvironmentSummaryCalculator.calculate(
            List.of(new BigDecimal("24.0"), new BigDecimal("26.0")),
            List.of(new BigDecimal("60.0"), new BigDecimal("60.0")));

    assertThat(response.avgTemperature()).isEqualByComparingTo("25.0");
  }

  @Test
  @DisplayName("[REQ-20-12] 기록 여러 건이면 avg_humidity 가 산술 평균이다")
  void req_20_12_multipleRecordsAverageHumidityIsArithmeticMean() {
    EnvironmentDailySummaryResponse response =
        EnvironmentSummaryCalculator.calculate(
            List.of(new BigDecimal("24.0"), new BigDecimal("24.0")),
            List.of(new BigDecimal("60.0"), new BigDecimal("70.0")));

    assertThat(response.avgHumidity()).isEqualByComparingTo("65.0");
  }

  @Test
  @DisplayName("[REQ-20-13] record_count 는 입력 건수와 같다")
  void req_20_13_recordCountEqualsInputSize() {
    EnvironmentDailySummaryResponse response =
        EnvironmentSummaryCalculator.calculate(
            List.of(new BigDecimal("24.0"), new BigDecimal("25.0"), new BigDecimal("26.0")),
            List.of(new BigDecimal("60.0"), new BigDecimal("61.0"), new BigDecimal("62.0")));

    assertThat(response.recordCount()).isEqualTo(3);
  }
}
