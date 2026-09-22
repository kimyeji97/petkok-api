package com.petkok.data.environment.dto;

import java.math.BigDecimal;

/**
 * {@code GET /pets/{pet_id}/environment/daily-summary} 응답 (Notion 「🦎 일간 평균」 행). 기록 0건이면 두 평균 모두
 * {@code null}, {@code recordCount} 는 0이다. 검증 계약 REQ-20-09 ~ 15.
 */
public record EnvironmentDailySummaryResponse(
    BigDecimal avgTemperature, BigDecimal avgHumidity, int recordCount) {}
