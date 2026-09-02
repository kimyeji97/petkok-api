package com.petkok.data.shed.entity;

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
 * 탈피 기록 ({@code shed_records}, 🦎 게코 전용). {@code WeightLog} 와 같은 형태 — {@code deleted_at} 없음(D7) ·
 * {@code petId} 는 UUID 컬럼({@code @ManyToOne} 아님).
 *
 * <p>종별 제한(게코 외 → {@code FEATURE_NOT_SUPPORTED_SPECIES})은 {@code ShedService} 가 진입 시 한다 — 여기서는 검증하지
 * 않는다.
 */
@Entity
@Table(name = "shed_records")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ShedRecord extends BaseCreatedEntity {

  @Id
  @GeneratedValue
  @Column(name = "id", nullable = false, updatable = false)
  private UUID id;

  @Column(name = "pet_id", nullable = false, updatable = false)
  private UUID petId;

  @Column(name = "shed_date", nullable = false)
  private LocalDate shedDate;

  @Column(name = "is_complete", nullable = false)
  private boolean isComplete;

  @Column(name = "is_assisted", nullable = false)
  private boolean isAssisted;

  @Column(name = "memo", length = 500)
  private String memo;

  private ShedRecord(
      UUID petId, LocalDate shedDate, boolean isComplete, boolean isAssisted, String memo) {
    this.petId = petId;
    this.shedDate = shedDate;
    this.isComplete = isComplete;
    this.isAssisted = isAssisted;
    this.memo = memo;
  }

  public static ShedRecord of(
      UUID petId, LocalDate shedDate, boolean isComplete, boolean isAssisted, String memo) {
    return new ShedRecord(petId, shedDate, isComplete, isAssisted, memo);
  }

  /**
   * 수정. <b>받은 값을 그대로 쓴다</b> — {@code null} 에 "변경 없음" 의미를 두지 않는다 (AGENTS §5). 부분 반영 병합은 {@code
   * ShedService} 가 한다.
   */
  public void update(LocalDate shedDate, boolean isComplete, boolean isAssisted, String memo) {
    this.shedDate = shedDate;
    this.isComplete = isComplete;
    this.isAssisted = isAssisted;
    this.memo = memo;
  }
}
