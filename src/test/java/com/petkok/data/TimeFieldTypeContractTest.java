package com.petkok.data;

import static org.assertj.core.api.Assertions.assertThat;

import com.petkok.data.activity.entity.ActivityLog;
import com.petkok.data.auth.entity.RefreshToken;
import com.petkok.data.common.entity.BaseCreatedEntity;
import com.petkok.data.common.entity.BaseSoftDeleteEntity;
import com.petkok.data.common.entity.BaseTimeEntity;
import com.petkok.data.pet.entity.Pet;
import com.petkok.data.weight.entity.WeightLog;
import java.lang.reflect.Field;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 엔티티 시각·날짜 필드의 타입 계약. 검증 계약 REQ-16-06 · 07 (PLAN-REQ-16 § 검증 계약).
 *
 * <p><b>REQ-16-06 은 Phase 1 전까지 실패한다.</b> 여섯 필드가 아직 {@code LocalDateTime} 이다.
 *
 * <p>06 이 <b>"{@code OffsetDateTime} 이다"가 아니라 "{@code LocalDateTime} 이 아니다"를 단언하는 이유</b> — 계획서
 * 범위—포함이 타입을 {@code D2 가 정하는 타입} 으로 열어 둔 채 케이스를 먼저 고정했기 때문이다. 여기서 잡아야 하는 사고는 <b>빠뜨린 필드</b>이고, 그건 고른
 * 타입이 무엇이든 같다. ({@code OffsetDateTime} 여부는 REQ-16-13 이 따로 단언한다 — D2 는 2026-08-28 Phase 0 프로브로
 * 확정됐다.)
 */
class TimeFieldTypeContractTest {

  private record Target(Class<?> owner, String field) {
    Class<?> type() {
      try {
        Field f = owner.getDeclaredField(field);
        f.setAccessible(true);
        return f.getType();
      } catch (NoSuchFieldException e) {
        throw new AssertionError(owner.getSimpleName() + "." + field + " 필드가 없다", e);
      }
    }

    @Override
    public String toString() {
      return owner.getSimpleName() + "." + field;
    }
  }

  /** 계획서 범위—포함이 열거한 시각 필드 6개. */
  private static final List<Target> TIME_FIELDS =
      List.of(
          new Target(BaseCreatedEntity.class, "createdAt"),
          new Target(BaseTimeEntity.class, "updatedAt"),
          new Target(BaseSoftDeleteEntity.class, "deletedAt"),
          new Target(RefreshToken.class, "expiresAt"),
          new Target(RefreshToken.class, "revokedAt"),
          new Target(ActivityLog.class, "loggedAt"));

  /**
   * 계획서 범위—제외의 날짜 컬럼 5개 중 <b>엔티티가 존재하는 3개.</b>
   *
   * <p>{@code entryDate}(diary) · {@code shedDate}(shed) 는 도메인 자체가 아직 없다 — REQ-10 Phase 3~5 다. 없는
   * 클래스를 참조하면 테스트 소스 전체가 컴파일되지 않으므로 여기서 빼고, 그 도메인이 들어올 때 이 목록에 추가한다. <b>지금 이 케이스는 5개가 아니라 3개를
   * 덮는다.</b>
   */
  private static final List<Target> DATE_FIELDS =
      List.of(
          new Target(WeightLog.class, "measuredAt"),
          new Target(Pet.class, "birthday"),
          new Target(Pet.class, "adoptionDate"));

  @Test
  @DisplayName("[REQ-16-06] 엔티티 시각 필드에 LocalDateTime 이 남아 있지 않다")
  void req_16_06_noLocalDateTimeLeftInTimeFields() {
    assertThat(TIME_FIELDS)
        .as("LocalDateTime 은 JVM 기본 TZ 에 암묵 의존한다 (D5) — 하나라도 남으면 그 필드만 조용히 9시간 어긋난다")
        .noneMatch(t -> t.type() == LocalDateTime.class);
  }

  @Test
  @DisplayName("[REQ-16-13] 엔티티 시각 필드 타입이 OffsetDateTime 이다")
  void req_16_13_timeFieldsAreOffsetDateTime() {
    assertThat(TIME_FIELDS)
        .as(
            "Instant 는 Jackson 이 항상 Z 로 내보내 D3 과 충돌하고(Phase 0 실측),"
                + " ZonedDateTime 은 timestamptz 가 잃어버리는 zone id 를 담는 척한다")
        .allMatch(t -> t.type() == OffsetDateTime.class);
  }

  @Test
  @DisplayName("[REQ-16-07] 날짜 필드는 여전히 LocalDate 다")
  void req_16_07_dateFieldsStayLocalDate() {
    assertThat(DATE_FIELDS)
        .as("날짜만 있는 값에는 타임존 개념이 없다. 바꾸면 커서 정렬(REQ-10 D8)과 파생 필드 정의가 흔들린다")
        .allMatch(t -> t.type() == LocalDate.class);
  }
}
