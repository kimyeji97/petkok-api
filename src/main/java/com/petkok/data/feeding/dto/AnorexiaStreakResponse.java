package com.petkok.data.feeding.dto;

import com.petkok.data.feeding.enums.StreakLevel;
import java.time.OffsetDateTime;

/**
 * {@code GET /pets/{pet_id}/feeding/anorexia-streak} 응답. 저장하지 않고 조회 시 계산한다 — {@code
 * AnorexiaStreakCalculator} 가 채운다. 검증 계약 REQ-10-59 ~ 65.
 */
public record AnorexiaStreakResponse(
    int currentStreakDays, StreakLevel level, OffsetDateTime lastEatenAt) {}
