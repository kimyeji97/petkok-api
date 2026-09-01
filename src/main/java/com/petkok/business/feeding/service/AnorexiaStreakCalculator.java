package com.petkok.business.feeding.service;

import com.petkok.data.feeding.dto.AnorexiaStreakResponse;
import com.petkok.data.feeding.enums.StreakLevel;
import com.petkok.framework.constant.TimeConstant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;

/**
 * 거식 스트릭 계산 — <b>I/O 없는 순수 클래스</b>(「소스 구조」 §1-4·§8). 저장하지 않고 조회 시 계산한다. 검증 계약 REQ-10-59 ~ 65
 * (PLAN-REQ-10 § 검증 계약).
 *
 * <p>마지막 정상 급여 시각(없으면 {@code null})부터 기준 시각까지의 <b>KST 달력 일수</b>로 판정한다 — {@code >= 7} {@link
 * StreakLevel#DANGER}, {@code >= 3} {@link StreakLevel#CAUTION}, 나머지 {@link StreakLevel#NONE}. 거식
 * "시도 건수"는 파라미터에 없다 — 일수 기준이라 건수가 계산에 들어올 자리가 없다(PLAN-REQ-10 미결 질문 Phase 3, "연속 거식 건수 안은 기각"). "기록은
 * 있지만 전부 거식"인 경우도 {@code lastEatenAt = null} 로 들어와 0건과 같은 결과가 된다.
 */
public final class AnorexiaStreakCalculator {

  private AnorexiaStreakCalculator() {}

  public static AnorexiaStreakResponse calculate(OffsetDateTime lastEatenAt, OffsetDateTime now) {
    if (lastEatenAt == null) {
      return new AnorexiaStreakResponse(0, StreakLevel.NONE, null);
    }

    LocalDate lastDate = lastEatenAt.atZoneSameInstant(TimeConstant.KST).toLocalDate();
    LocalDate nowDate = now.atZoneSameInstant(TimeConstant.KST).toLocalDate();
    long days = ChronoUnit.DAYS.between(lastDate, nowDate);

    StreakLevel level;
    if (days >= 7) {
      level = StreakLevel.DANGER;
    } else if (days >= 3) {
      level = StreakLevel.CAUTION;
    } else {
      level = StreakLevel.NONE;
    }

    return new AnorexiaStreakResponse((int) days, level, lastEatenAt);
  }
}
