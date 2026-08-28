package com.petkok.data.activity.entity;

import com.petkok.data.activity.enums.ActivityType;
import com.petkok.data.common.entity.BaseCreatedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
 * 활동 기록 ({@code activity_logs}). {@code WeightLog} 와 같은 형태 — {@code deleted_at} 없음(D7) · {@code
 * petId} 는 UUID 컬럼({@code @ManyToOne} 아님).
 *
 * <p>{@code distanceKm} 는 <b>종·유형과 무관하게 받은 값을 그대로 둔다</b> (D13). "게코 미사용"은 UI 규약이지 서버 거부 규약이 아니다.
 */
@Entity
@Table(name = "activity_logs")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ActivityLog extends BaseCreatedEntity {

  @Id
  @GeneratedValue
  @Column(name = "id", nullable = false, updatable = false)
  private UUID id;

  @Column(name = "pet_id", nullable = false, updatable = false)
  private UUID petId;

  @Enumerated(EnumType.STRING)
  @Column(name = "activity_type", nullable = false, length = 50)
  private ActivityType activityType;

  @Column(name = "duration_minutes")
  private Integer durationMinutes;

  @Column(name = "distance_km", precision = 6, scale = 2)
  private BigDecimal distanceKm;

  @Column(name = "memo", columnDefinition = "text")
  private String memo;

  @Column(name = "logged_at", nullable = false)
  private OffsetDateTime loggedAt;

  private ActivityLog(
      UUID petId,
      ActivityType activityType,
      Integer durationMinutes,
      BigDecimal distanceKm,
      String memo,
      OffsetDateTime loggedAt) {
    this.petId = petId;
    this.activityType = activityType;
    this.durationMinutes = durationMinutes;
    this.distanceKm = distanceKm;
    this.memo = memo;
    this.loggedAt = loggedAt;
  }

  public static ActivityLog of(
      UUID petId,
      ActivityType activityType,
      Integer durationMinutes,
      BigDecimal distanceKm,
      String memo,
      OffsetDateTime loggedAt) {
    return new ActivityLog(petId, activityType, durationMinutes, distanceKm, memo, loggedAt);
  }

  /** 수정. <b>받은 값을 그대로 쓴다</b> — 병합·종 검증은 {@code ActivityService} 가 한다. */
  public void update(
      ActivityType activityType,
      Integer durationMinutes,
      BigDecimal distanceKm,
      String memo,
      OffsetDateTime loggedAt) {
    this.activityType = activityType;
    this.durationMinutes = durationMinutes;
    this.distanceKm = distanceKm;
    this.memo = memo;
    this.loggedAt = loggedAt;
  }
}
