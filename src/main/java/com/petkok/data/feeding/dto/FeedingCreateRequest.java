package com.petkok.data.feeding.dto;

import com.petkok.data.feeding.enums.FoodSize;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * {@code POST /pets/{pet_id}/feeding} 요청. 원본 Validation — "`fed_at` 필수 / `is_refused` 필수 /
 * `food_size` 선택: `S | M | L`". 검증 계약 REQ-10-52 · 53 · 56.
 *
 * <p>{@code isRefused} 는 <b>{@code Boolean}(boxed)</b> 이다 — primitive {@code boolean} 이면 필드 누락 시
 * Jackson 이 조용히 {@code false} 로 채워 REQ-10-52("`is_refused` 누락 → 400")가 성립하지 않는다.
 */
public record FeedingCreateRequest(
    String foodType,
    FoodSize foodSize,
    BigDecimal amount,
    String amountUnit,
    @NotNull Boolean isRefused,
    @NotNull OffsetDateTime fedAt,
    String memo) {}
