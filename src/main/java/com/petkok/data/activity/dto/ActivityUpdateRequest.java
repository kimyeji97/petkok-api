package com.petkok.data.activity.dto;

import com.petkok.data.activity.enums.ActivityType;
import jakarta.validation.constraints.Digits;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * {@code PATCH /pets/{pet_id}/activity/{log_id}} 요청. <b>보낸 필드만 반영</b> (D10). ⚠️
 * {@code @NotNull}·{@code @NotBlank} 금지 (AGENTS §5). {@code activityType} 이 오면 종 검증이 다시 걸린다
 * (REQ-10-29). 검증 계약 REQ-10-38.
 */
public record ActivityUpdateRequest(
    ActivityType activityType,
    Integer durationMinutes,
    @Digits(integer = 4, fraction = 2) BigDecimal distanceKm,
    String memo,
    OffsetDateTime loggedAt) {}
