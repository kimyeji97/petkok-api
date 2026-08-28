package com.petkok.data.activity.dto;

import com.petkok.data.activity.enums.ActivityType;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * {@code POST /pets/{pet_id}/activity} 요청. 원본 Validation — "`activity_type` 필수 / `distance_km` 선택 /
 * `logged_at` 필수". 종별 허용값 검증은 서비스가 한다. {@code @Digits} 는 DB {@code decimal(6,2)} 초과가 500 으로 새는 것을
 * 막는다.
 */
public record ActivityCreateRequest(
    @NotNull ActivityType activityType,
    Integer durationMinutes,
    @Digits(integer = 4, fraction = 2) BigDecimal distanceKm,
    String memo,
    @NotNull OffsetDateTime loggedAt) {}
