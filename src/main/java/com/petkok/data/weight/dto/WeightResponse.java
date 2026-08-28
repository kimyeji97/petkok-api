package com.petkok.data.weight.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 체중 기록 응답. 목록 항목과 201 응답이 같은 형태다 (Notion 「체중 목록」·「체중 기록」 행).
 *
 * <p><b>파생 필드 (D3, 2026-08-28 확정)</b> — 저장하지 않고 조회 시 계산.
 *
 * <ul>
 *   <li>{@code weightChangeRate} — 직전 기록 대비 변화율(%), 소수 1자리. 직전이 없으면(첫 기록) {@code null}
 *   <li>{@code isWeightWarning} — {@code |변화율| >= 20} 이면 {@code true}. 직전이 없으면 {@code false}
 * </ul>
 */
public record WeightResponse(
    UUID id,
    UUID petId,
    Integer weightG,
    LocalDate measuredAt,
    String memo,
    Double weightChangeRate,
    boolean isWeightWarning,
    LocalDateTime createdAt) {}
