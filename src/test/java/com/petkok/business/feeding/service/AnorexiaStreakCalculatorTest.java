package com.petkok.business.feeding.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.petkok.data.feeding.dto.AnorexiaStreakResponse;
import com.petkok.data.feeding.enums.StreakLevel;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 거식 스트릭 계산기 — <b>I/O 없는 순수 클래스</b>(「소스 구조」 §1-4·§8). 검증 계약 REQ-10-59 ~ 65 (PLAN-REQ-10 §
 * 검증 계약).
 *
 * <p>⚠️ 이 파일은 {@code AnorexiaStreakCalculator} 가 아직 없어 컴파일되지 않는다 — {@code /implement REQ-10
 * 3} 이 만든다.
 *
 * <p>가정한 계약 — {@code AnorexiaStreakCalculator.calculate(OffsetDateTime lastEatenAt, OffsetDateTime
 * now)} 는 마지막 정상 급여 시각(없으면 {@code null})과 기준 시각만 받는 <b>순수 정적 메서드</b>다. 거식 시도 건수는 아예 파라미터로
 * 받지 않는다 — "일수" 정의(미결 질문)가 "건수" 정의와 애초에 갈릴 수 없는 시그니처다. 날짜 변환은 {@code TimeConstant.KST} 를 쓴다.
 */
class AnorexiaStreakCalculatorTest {

  // 기준 시각 — 2026-07-07T12:00:00+09:00
  private static final OffsetDateTime NOW = OffsetDateTime.parse("2026-07-07T12:00:00+09:00");

  @Test
  @DisplayName("[REQ-10-59] 기록 0건(lastEatenAt=null)이면 {0, NONE, null} 이다")
  void req_10_59_noRecordsReturnsZeroNoneNull() {
    AnorexiaStreakResponse response = AnorexiaStreakCalculator.calculate(null, NOW);

    assertThat(response)
        .isEqualTo(new AnorexiaStreakResponse(0, StreakLevel.NONE, null));
  }

  @Test
  @DisplayName("[REQ-10-60] 2일 경과는 NONE 이다")
  void req_10_60_twoDaysIsNone() {
    OffsetDateTime lastEatenAt = NOW.minusDays(2);

    AnorexiaStreakResponse response = AnorexiaStreakCalculator.calculate(lastEatenAt, NOW);

    assertThat(response.level()).isEqualTo(StreakLevel.NONE);
  }

  @Test
  @DisplayName("[REQ-10-61] 3일 경과는 CAUTION 경계다")
  void req_10_61_threeDaysIsCaution() {
    OffsetDateTime lastEatenAt = NOW.minusDays(3);

    AnorexiaStreakResponse response = AnorexiaStreakCalculator.calculate(lastEatenAt, NOW);

    assertThat(response.level()).isEqualTo(StreakLevel.CAUTION);
  }

  @Test
  @DisplayName("[REQ-10-62] 6일 경과는 CAUTION 이다 (아직 DANGER 아님)")
  void req_10_62_sixDaysIsCaution() {
    OffsetDateTime lastEatenAt = NOW.minusDays(6);

    AnorexiaStreakResponse response = AnorexiaStreakCalculator.calculate(lastEatenAt, NOW);

    assertThat(response.level()).isEqualTo(StreakLevel.CAUTION);
  }

  @Test
  @DisplayName("[REQ-10-63] 7일 경과는 DANGER 경계다")
  void req_10_63_sevenDaysIsDanger() {
    OffsetDateTime lastEatenAt = NOW.minusDays(7);

    AnorexiaStreakResponse response = AnorexiaStreakCalculator.calculate(lastEatenAt, NOW);

    assertThat(response.level()).isEqualTo(StreakLevel.DANGER);
  }

  @Test
  @DisplayName("[REQ-10-64] 자정을 넘기면 KST 달력 일수가 1 증가한다 (23:30 → 익일 00:30)")
  void req_10_64_midnightCrossingCountsAsOneCalendarDay() {
    OffsetDateTime lastEatenAt = OffsetDateTime.parse("2026-06-30T23:30:00+09:00");
    OffsetDateTime now = OffsetDateTime.parse("2026-07-01T00:30:00+09:00");

    AnorexiaStreakResponse response = AnorexiaStreakCalculator.calculate(lastEatenAt, now);

    assertThat(response.currentStreakDays()).isEqualTo(1);
  }

  @Test
  @DisplayName("[REQ-10-65] 경과일이 0일이면 거식 시도 건수와 무관하게 NONE 이다")
  void req_10_65_zeroDaysIsNoneRegardlessOfAttemptCount() {
    // 계산기는 거식 "시도 건수"를 아예 파라미터로 받지 않는다 — 그 사이 시도가 몇 건이든
    // lastEatenAt·now 가 같으면(0일 경과) 결과는 항상 NONE 이다.
    AnorexiaStreakResponse response = AnorexiaStreakCalculator.calculate(NOW, NOW);

    assertThat(response.level()).isEqualTo(StreakLevel.NONE);
  }
}
