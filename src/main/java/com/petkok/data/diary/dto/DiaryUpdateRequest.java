package com.petkok.data.diary.dto;

import com.petkok.data.diary.enums.ConditionTag;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/**
 * {@code PATCH /pets/{pet_id}/diary/{entry_id}} 요청. <b>보낸 필드만 반영된다</b> (D10).
 *
 * <p>⚠️ <b>{@code @NotNull}·{@code @NotBlank} 를 붙이지 않는다</b> (AGENTS §5). 검증 계약 REQ-10-100.
 */
public record DiaryUpdateRequest(
    @Size(max = 200) String title,
    String content,
    ConditionTag conditionTag,
    LocalDate entryDate) {}
