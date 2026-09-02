package com.petkok.data.shed.dto;

import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/**
 * {@code PATCH /pets/{pet_id}/shed/{record_id}} 요청. <b>보낸 필드만 반영된다</b> (D10).
 *
 * <p>⚠️ <b>{@code @NotNull}·{@code @NotBlank} 를 붙이지 않는다</b> (AGENTS §5). 검증 계약 REQ-10-74.
 */
public record ShedUpdateRequest(
    LocalDate shedDate, Boolean isComplete, Boolean isAssisted, @Size(max = 500) String memo) {}
