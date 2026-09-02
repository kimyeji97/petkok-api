package com.petkok.data.shed.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/**
 * {@code POST /pets/{pet_id}/shed} 요청. 원본 Validation — "`shed_date` 필수". 검증 계약 REQ-10-77.
 *
 * <p>{@code isComplete}·{@code isAssisted} 는 선택 — 안 보내면 서비스가 DB 기본값(각각 {@code true}·{@code false})을
 * 적용한다.
 */
public record ShedCreateRequest(
    @NotNull LocalDate shedDate,
    Boolean isComplete,
    Boolean isAssisted,
    @Size(max = 500) String memo) {}
