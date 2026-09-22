package com.petkok.data.gallery.dto;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 갤러리 사진 응답. 목록 항목과 201 응답이 같은 형태다.
 *
 * <p>{@code tags}·{@code isRepresentative} 는 REQ-22 추가.
 */
public record PhotoResponse(
    UUID id,
    UUID petId,
    UUID diaryEntryId,
    String imageUrl,
    String caption,
    LocalDate takenDate,
    List<String> tags,
    boolean isRepresentative,
    OffsetDateTime createdAt) {}
