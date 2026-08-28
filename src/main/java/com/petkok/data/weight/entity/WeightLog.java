package com.petkok.data.weight.entity;

import com.petkok.data.common.entity.BaseCreatedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 체중 기록 ({@code weight_logs}). 검증 계약 REQ-10-16 · 17 (PLAN-REQ-10 § 검증 계약).
 *
 * <p><b>{@code deleted_at} 이 없다 — 삭제는 행 삭제다</b> (D7). {@code users}·{@code pets} 만 소프트 딜리트다.
 *
 * <p><b>{@code petId} 는 {@code UUID} 컬럼이고 {@code @ManyToOne} 이 아니다.</b> {@code Pet} 은 {@code
 * data/pet} 에 있어 연관관계를 걸면 {@code data/weight → data/pet.entity} 참조가 되어 ArchUnit 에 걸린다({@code
 * RefreshToken} 과 같은 이유). 소유권은 {@code PetAccessGuard} 가 판정하므로 여기서 Pet 으로 탐색할 일이 없다.
 *
 * <p>체중 변화율·경고는 <b>저장하지 않는다</b> (D3) — 조회 시 {@code WeightService} 가 직전 기록과 비교해 계산한다.
 */
@Entity
@Table(name = "weight_logs")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WeightLog extends BaseCreatedEntity {

  @Id
  @GeneratedValue
  @Column(name = "id", nullable = false, updatable = false)
  private UUID id;

  @Column(name = "pet_id", nullable = false, updatable = false)
  private UUID petId;

  /** 그램 단위 통일 (게코 수십 g ~ 대형견 수십 kg). */
  @Column(name = "weight_g", nullable = false)
  private Integer weightG;

  @Column(name = "measured_at", nullable = false)
  private LocalDate measuredAt;

  @Column(name = "memo", length = 500)
  private String memo;

  private WeightLog(UUID petId, Integer weightG, LocalDate measuredAt, String memo) {
    this.petId = petId;
    this.weightG = weightG;
    this.measuredAt = measuredAt;
    this.memo = memo;
  }

  public static WeightLog of(UUID petId, Integer weightG, LocalDate measuredAt, String memo) {
    return new WeightLog(petId, weightG, measuredAt, memo);
  }

  /**
   * 수정. <b>받은 값을 그대로 쓴다</b> — {@code null} 에 "변경 없음" 의미를 두지 않는다 (AGENTS §5). 부분 반영 병합은 {@code
   * WeightService} 가 한다.
   */
  public void update(Integer weightG, LocalDate measuredAt, String memo) {
    this.weightG = weightG;
    this.measuredAt = measuredAt;
    this.memo = memo;
  }
}
