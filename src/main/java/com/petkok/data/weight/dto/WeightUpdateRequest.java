package com.petkok.data.weight.dto;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/**
 * {@code PATCH /pets/{pet_id}/weight/{log_id}} 요청. <b>보낸 필드만 반영된다</b> (D10).
 *
 * <p>⚠️ <b>{@code @NotBlank}·{@code @NotNull} 을 붙이지 않는다</b> (AGENTS §5). {@code null} 은 "변경 없음"이다.
 * {@code @Positive}·{@code @Size} 는 {@code null} 을 통과시킨다. 검증 계약 REQ-10-15.
 */
public record WeightUpdateRequest(
    @Positive Integer weightG, LocalDate measuredAt, @Size(max = 500) String memo) {}
