package com.petkok.data.feeding.entity;

import com.petkok.data.common.entity.BaseCreatedEntity;
import com.petkok.data.feeding.enums.FoodSize;
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
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 급여 기록 ({@code feeding_logs}). {@code ActivityLog} 와 같은 형태 — {@code deleted_at} 없음(D7) · {@code
 * petId} 는 UUID 컬럼({@code @ManyToOne} 아님).
 *
 * <p>{@code foodSize} 는 <b>종과 무관하게 받은 값을 그대로 둔다</b> — "개/고양이 미사용"은 UI 규약이지 서버 거부 규약이 아니다
 * (PLAN-REQ-10 미결 질문 Phase 3, D13 과 같은 결).
 *
 * <p>⚠️ <b>{@code @Builder} 로 {@code id} 를 세팅하지 말 것.</b> 필드가 8개(petId·foodType·foodSize·amount·
 * amountUnit·isRefused·fedAt·memo)라 정적 팩토리로는 Checkstyle {@code ParameterNumber}(최대 7)를 만족할 수 없어
 * {@code @Builder} + {@code @AllArgsConstructor} 를 골랐는데, 그 부작용으로 <b>빌더에 {@code id} 가 노출된다.</b> 채우면
 * Spring Data 가 "ID 가 있으니 기존 엔티티"로 보고 {@code save()} 를 merge 로 처리해 INSERT 전에 SELECT 를 한 번 더 날린다
 * ({@code Pet} 에도 같은 주의가 있다). <b>ID 는 Hibernate 가 채운다.</b>
 */
@Entity
@Table(name = "feeding_logs")
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FeedingLog extends BaseCreatedEntity {

  @Id
  @GeneratedValue
  @Column(name = "id", nullable = false, updatable = false)
  private UUID id;

  @Column(name = "pet_id", nullable = false, updatable = false)
  private UUID petId;

  @Column(name = "food_type", length = 100)
  private String foodType;

  @Enumerated(EnumType.STRING)
  @Column(name = "food_size")
  private FoodSize foodSize;

  @Column(name = "amount", precision = 8, scale = 2)
  private BigDecimal amount;

  @Column(name = "amount_unit", length = 20)
  private String amountUnit;

  @Column(name = "is_refused", nullable = false)
  private boolean isRefused;

  @Column(name = "fed_at", nullable = false)
  private OffsetDateTime fedAt;

  @Column(name = "memo", columnDefinition = "text")
  private String memo;

  /** 수정. <b>받은 값을 그대로 쓴다</b> — 병합은 {@code FeedingService} 가 한다. {@code petId} 는 불변이라 받지 않는다. */
  public void update(
      String foodType,
      FoodSize foodSize,
      BigDecimal amount,
      String amountUnit,
      boolean isRefused,
      OffsetDateTime fedAt,
      String memo) {
    this.foodType = foodType;
    this.foodSize = foodSize;
    this.amount = amount;
    this.amountUnit = amountUnit;
    this.isRefused = isRefused;
    this.fedAt = fedAt;
    this.memo = memo;
  }
}
