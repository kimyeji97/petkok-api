package com.petkok.data.weight.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/**
 * {@code POST /pets/{pet_id}/weight} 요청. 원본 Validation — "`weight_g` 필수 (양의 정수) / `measured_at`
 * 필수". 검증 계약 REQ-10-12 ~ 14.
 */
public record WeightCreateRequest(
    @NotNull @Positive Integer weightG,
    @NotNull LocalDate measuredAt,
    @Size(max = 500) String memo) {}
