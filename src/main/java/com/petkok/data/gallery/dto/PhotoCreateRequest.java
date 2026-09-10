package com.petkok.data.gallery.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.UUID;

/**
 * {@code POST /pets/{pet_id}/photos} 요청 — 업로드 완료 후 메타데이터 저장. 계획서 범위 — "`image_url`(필수) ·
 * `caption`·`taken_at`·`diary_entry_id`(전부 선택)". 검증 계약 REQ-11-05 · 06 · 09 · 10.
 *
 * <p>{@code diaryEntryId} 를 이 요청 시점에 받는 것도 계획서 결정이다 — 사후에 diary 쪽에서 연결하지 않는다.
 */
public record PhotoCreateRequest(
    @NotBlank String imageUrl,
    @Size(max = 500) String caption,
    LocalDate takenAt,
    UUID diaryEntryId) {}
