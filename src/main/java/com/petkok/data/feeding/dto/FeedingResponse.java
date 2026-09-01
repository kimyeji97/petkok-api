package com.petkok.data.feeding.dto;

import com.petkok.data.feeding.enums.FoodSize;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/** 급여 기록 응답 (Notion 「급여 목록」·「급여 기록」 행). */
public record FeedingResponse(
    UUID id,
    UUID petId,
    String foodType,
    FoodSize foodSize,
    BigDecimal amount,
    String amountUnit,
    boolean isRefused,
    OffsetDateTime fedAt,
    String memo,
    OffsetDateTime createdAt) {}
