package com.petkok.data.diary.dto;

import com.petkok.data.diary.enums.ConditionTag;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/**
 * {@code POST /pets/{pet_id}/diary} 요청. 원본 Validation — "`entry_date` 필수". 검증 계약 REQ-10-103 · 104.
 *
 * <p>⚠️ <b>{@code photo_ids} 필드가 없다 — 의도적이다</b> (D4). Spring Boot 기본값(unknown 프로퍼티 무시)이라 요청에 실려 와도
 * 그냥 무시된다(REQ-10-110). 필드를 선언하지 않는 것 자체가 "무시" 구현이다.
 */
public record DiaryCreateRequest(
    @Size(max = 200) String title,
    String content,
    ConditionTag conditionTag,
    @NotNull LocalDate entryDate) {}
