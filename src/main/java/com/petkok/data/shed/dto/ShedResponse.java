package com.petkok.data.shed.dto;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

/** 탈피 기록 응답 (Notion 「탈피 목록」·「탈피 기록」 행). */
public record ShedResponse(
    UUID id,
    UUID petId,
    LocalDate shedDate,
    boolean isComplete,
    boolean isAssisted,
    String memo,
    OffsetDateTime createdAt) {}
