package com.petkok.business.shed.service;

import com.petkok.data.shed.dto.ShedPredictionResponse;
import com.petkok.data.shed.enums.PredictionConfidence;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * 탈피 예측 계산 — <b>I/O 없는 순수 클래스</b>(「소스 구조」 §1-4·§8). 저장하지 않고 조회 시 계산한다. 검증 계약 REQ-10-84 ~ 91
 * (PLAN-REQ-10 § 검증 계약).
 *
 * <p>{@code recentShedDatesDesc} 는 {@code shed_date} 내림차순(최신이 먼저)으로 정렬된 리스트를 받는다고 가정한다 — {@code
 * ShedRecordRepository.findFirstPage} 가 이미 그 순서로 정렬해 돌려준다. <b>최근 3건까지만</b> 써서 간격 평균을 낸다 — 4건 이상이면
 * 가장 오래된 것부터 버린다.
 *
 * <p>기록 1건이면 간격 자체가 없어 {@code average_cycle_days}·{@code predicted_date} 가 {@code null} 이다. 고정 기본
 * 주기 상수는 쓰지 않는다 — 성장 단계별 탈피 주기 편차가 10배 넘게 커 단일 상수가 부적절하다는 게 근거다(사용자 확정 2026-09-02, {@code
 * docs/reference/gecko-growth-and-shed-cycle.md} 참고).
 */
public final class ShedPredictionCalculator {

  private ShedPredictionCalculator() {}

  public static ShedPredictionResponse calculate(List<LocalDate> recentShedDatesDesc) {
    int total = recentShedDatesDesc.size();
    if (total == 0) {
      return new ShedPredictionResponse(null, null, 0, PredictionConfidence.LOW);
    }
    if (total == 1) {
      return new ShedPredictionResponse(null, null, 1, PredictionConfidence.LOW);
    }

    List<LocalDate> recent = total > 3 ? recentShedDatesDesc.subList(0, 3) : recentShedDatesDesc;
    int basedOnRecords = recent.size();
    int gapCount = basedOnRecords - 1;

    long sum = 0;
    for (int i = 0; i < gapCount; i++) {
      sum += ChronoUnit.DAYS.between(recent.get(i + 1), recent.get(i));
    }
    int averageCycleDays = Math.toIntExact(Math.round(sum / (double) gapCount));
    LocalDate predictedDate = recent.get(0).plusDays(averageCycleDays);
    PredictionConfidence confidence =
        basedOnRecords >= 3 ? PredictionConfidence.HIGH : PredictionConfidence.MEDIUM;

    return new ShedPredictionResponse(predictedDate, averageCycleDays, basedOnRecords, confidence);
  }
}
