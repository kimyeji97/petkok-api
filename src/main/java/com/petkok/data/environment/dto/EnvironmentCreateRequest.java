package com.petkok.data.environment.dto;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * {@code POST /pets/{pet_id}/environment} 요청. Notion 「환경 기록」 행 Validation — "`temperature` 필수 /
 * `humidity` 필수 / `measured_at` 필수". 검증 계약 REQ-20-04(PLAN-REQ-20 § 검증 계약).
 */
public record EnvironmentCreateRequest(
    @NotNull BigDecimal temperature,
    @NotNull BigDecimal humidity,
    @NotNull OffsetDateTime measuredAt,
    String memo) {}
