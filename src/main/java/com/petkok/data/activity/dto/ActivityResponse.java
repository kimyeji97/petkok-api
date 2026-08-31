package com.petkok.data.activity.dto;

import com.petkok.data.activity.enums.ActivityType;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/** 활동 기록 응답 (Notion 「활동 목록」 항목 · 201 응답). */
public record ActivityResponse(
    UUID id,
    UUID petId,
    ActivityType activityType,
    Integer durationMinutes,
    BigDecimal distanceKm,
    String memo,
    OffsetDateTime loggedAt,
    OffsetDateTime createdAt) {}
