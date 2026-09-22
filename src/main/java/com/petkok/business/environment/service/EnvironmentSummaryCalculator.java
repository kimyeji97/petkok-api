package com.petkok.business.environment.service;

import com.petkok.data.environment.dto.EnvironmentDailySummaryResponse;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * 일간 온습도 평균 계산 — <b>I/O 없는 순수 클래스</b>(「소스 구조」 §1-4·§8, {@code ShedPredictionCalculator}·{@code
 * AnorexiaStreakCalculator}와 같은 패턴). 저장하지 않고 조회 시 계산한다. 검증 계약 REQ-20-09 ~ 13(PLAN-REQ-20 § 검증 계약).
 *
 * <p>두 리스트는 같은 길이이고 같은 인덱스가 한 기록의 온도·습도 쌍이라고 가정한다.
 */
public final class EnvironmentSummaryCalculator {

  private static final int AVERAGE_SCALE = 1;

  private EnvironmentSummaryCalculator() {}

  public static EnvironmentDailySummaryResponse calculate(
      List<BigDecimal> temperatures, List<BigDecimal> humidities) {
    int recordCount = temperatures.size();
    if (recordCount == 0) {
      return new EnvironmentDailySummaryResponse(null, null, 0);
    }
    return new EnvironmentDailySummaryResponse(
        average(temperatures, recordCount), average(humidities, recordCount), recordCount);
  }

  private static BigDecimal average(List<BigDecimal> values, int count) {
    BigDecimal sum = BigDecimal.ZERO;
    for (BigDecimal value : values) {
      sum = sum.add(value);
    }
    return sum.divide(BigDecimal.valueOf(count), AVERAGE_SCALE, RoundingMode.HALF_UP);
  }
}
