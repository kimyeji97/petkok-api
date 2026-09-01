package com.petkok.data.feeding.dto;

import com.petkok.data.feeding.enums.FoodSize;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * {@code PATCH /pets/{pet_id}/feeding/{log_id}} 요청. <b>보낸 필드만 반영</b> (D10). ⚠️
 * {@code @NotNull}·{@code @NotBlank} 금지 (AGENTS §5). 검증 계약 REQ-10-49.
 */
public record FeedingUpdateRequest(
    String foodType,
    FoodSize foodSize,
    BigDecimal amount,
    String amountUnit,
    Boolean isRefused,
    OffsetDateTime fedAt,
    String memo) {}
