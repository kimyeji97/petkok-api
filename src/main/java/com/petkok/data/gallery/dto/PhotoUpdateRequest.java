package com.petkok.data.gallery.dto;

import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;

/**
 * {@code PATCH /pets/{pet_id}/photos/{photo_id}} 요청 — REQ-22 신설(이전엔 이 경로 자체가 없었다). <b>보낸 필드만
 * 반영</b>. ⚠️ {@code @NotNull}·{@code @NotBlank} 금지 (AGENTS §5) — 누락·{@code null} 은 "변경 없음"이다.
 *
 * <p>{@code tags} 는 전체 교체다 — {@code null}(누락)이면 기존 태그를 건드리지 않고, {@code []}(빈 배열)이면 전부 삭제한다(계획서 결정).
 * {@code isRepresentative} 는 boxed {@code Boolean} — {@code true} 로 보내면 같은 달의 기존 대표 사진은 자동 해제된다. 검증
 * 계약 REQ-22-10 ~ 15.
 */
public record PhotoUpdateRequest(
    @Size(max = 500) String caption,
    LocalDate takenDate,
    List<String> tags,
    Boolean isRepresentative) {}
