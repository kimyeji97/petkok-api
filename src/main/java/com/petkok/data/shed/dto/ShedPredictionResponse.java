package com.petkok.data.shed.dto;

import com.petkok.data.shed.enums.PredictionConfidence;
import java.time.LocalDate;

/**
 * {@code GET /pets/{pet_id}/shed/prediction} 응답. 저장하지 않고 조회 시 계산한다 — {@code
 * ShedPredictionCalculator} 가 채운다. 검증 계약 REQ-10-84 ~ 91.
 */
public record ShedPredictionResponse(
    LocalDate predictedDate,
    Integer averageCycleDays,
    int basedOnRecords,
    PredictionConfidence confidence) {}
