package com.petkok.data.diary.dto;

import com.petkok.data.diary.enums.ConditionTag;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * 다이어리 응답 (Notion 「다이어리 목록」·「다이어리 상세」 행).
 *
 * <p>⚠️ <b>{@code photos}·{@code photo_count} 필드가 없다 — 의도적이다</b> (D4, REQ-11 로 이관). 검증 계약
 * REQ-10-108 · 109. {@code updatedAt} 은 있다(D11) — 다른 기록 도메인과 다르다.
 */
public record DiaryResponse(
    UUID id,
    UUID petId,
    String title,
    String content,
    ConditionTag conditionTag,
    LocalDate entryDate,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt) {}
