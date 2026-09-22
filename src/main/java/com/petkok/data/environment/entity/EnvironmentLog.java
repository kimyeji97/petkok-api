package com.petkok.data.environment.entity;

import com.petkok.data.common.entity.BaseCreatedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 게코 사육 환경(온습도) 기록 ({@code environment_logs}, 🦎 게코 전용). {@code ShedRecord} 와 같은 형태 — {@code
 * deleted_at} 없음 · {@code petId} 는 UUID 컬럼({@code @ManyToOne} 아님).
 *
 * <p>종별 제한(게코 외 → {@code FEATURE_NOT_SUPPORTED_SPECIES})은 {@code EnvironmentService} 가 진입 시 한다 —
 * 여기서는 검증하지 않는다.
 */
@Entity
@Table(name = "environment_logs")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EnvironmentLog extends BaseCreatedEntity {

  @Id
  @GeneratedValue
  @Column(name = "id", nullable = false, updatable = false)
  private UUID id;

  @Column(name = "pet_id", nullable = false, updatable = false)
  private UUID petId;

  @Column(name = "temperature", nullable = false, precision = 4, scale = 1)
  private BigDecimal temperature;

  @Column(name = "humidity", nullable = false, precision = 4, scale = 1)
  private BigDecimal humidity;

  @Column(name = "measured_at", nullable = false)
  private OffsetDateTime measuredAt;

  @Column(name = "memo", length = 500)
  private String memo;

  private EnvironmentLog(
      UUID petId,
      BigDecimal temperature,
      BigDecimal humidity,
      OffsetDateTime measuredAt,
      String memo) {
    this.petId = petId;
    this.temperature = temperature;
    this.humidity = humidity;
    this.measuredAt = measuredAt;
    this.memo = memo;
  }

  public static EnvironmentLog of(
      UUID petId,
      BigDecimal temperature,
      BigDecimal humidity,
      OffsetDateTime measuredAt,
      String memo) {
    return new EnvironmentLog(petId, temperature, humidity, measuredAt, memo);
  }

  /**
   * 수정. <b>받은 값을 그대로 쓴다</b> — {@code null} 에 "변경 없음" 의미를 두지 않는다 (AGENTS §5). 부분 반영 병합은 {@code
   * EnvironmentService} 가 한다.
   */
  public void update(
      BigDecimal temperature, BigDecimal humidity, OffsetDateTime measuredAt, String memo) {
    this.temperature = temperature;
    this.humidity = humidity;
    this.measuredAt = measuredAt;
    this.memo = memo;
  }
}
