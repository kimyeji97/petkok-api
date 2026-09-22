package com.petkok.data.environment.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * {@code PATCH /pets/{pet_id}/environment/{log_id}} 요청. <b>보낸 필드만 반영</b>. ⚠️
 * {@code @NotNull}·{@code @NotBlank} 금지 (AGENTS §5) — 누락·{@code null} 은 "변경 없음"이다. 검증 계약 REQ-20-06.
 */
public record EnvironmentUpdateRequest(
    BigDecimal temperature, BigDecimal humidity, OffsetDateTime measuredAt, String memo) {}
