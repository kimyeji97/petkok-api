package com.petkok.data.diary.dto;

import com.petkok.data.diary.enums.ConditionTag;
import com.petkok.framework.port.PhotoSummary;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 다이어리 응답 (Notion 「다이어리 목록」·「다이어리 상세」 행).
 *
 * <p>{@code photos}(상세)·{@code photoCount}(목록)는 REQ-11 Phase 2에서 채워진다 — {@code PhotoLookup} 포트로
 * gallery 도메인을 조회한 결과다. 둘 다 채우지 않고 한쪽만 쓰는 이유는 REQ-10-108·109 완료 기준이 애초에 "상세는 photos, 목록은
 * photo_count"로 갈라 뒀기 때문이다 — 목록에서 매 항목마다 전체 사진 목록을 실으면 응답이 불필요하게 커진다. {@code updatedAt} 은 있다(D11) —
 * 다른 기록 도메인과 다르다.
 */
public record DiaryResponse(
    UUID id,
    UUID petId,
    String title,
    String content,
    ConditionTag conditionTag,
    LocalDate entryDate,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt,
    List<PhotoSummary> photos,
    Integer photoCount) {}
