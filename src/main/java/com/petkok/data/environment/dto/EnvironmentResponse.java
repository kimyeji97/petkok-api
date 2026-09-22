package com.petkok.data.environment.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/** 게코 사육 환경(온습도) 기록 응답 (Notion 「환경 기록」·「환경 목록」·「환경 수정」 행). */
public record EnvironmentResponse(
    UUID id,
    UUID petId,
    BigDecimal temperature,
    BigDecimal humidity,
    OffsetDateTime measuredAt,
    String memo,
    OffsetDateTime createdAt) {}
