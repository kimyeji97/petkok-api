package com.petkok.data.diary.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 다이어리 관찰 태그. **4종 확정**(D1) — 거식·탈피도와줌·탈피완료는 급여·탈피 기록에서 파생돼 여기 없다(단일 출처).
 *
 * <p>이 프로젝트 첫 <b>한글 값</b> enum이다. DB·Java 상수명은 영문(AGENTS §5 "상수 UPPER_SNAKE_CASE")을 지키고, JSON 왕복만
 * 한글로 한다 — {@link JsonValue}(직렬화)·{@link JsonCreator}(역직렬화)로 매핑한다(2026-09-02 확정). 매핑 밖 문자열은 {@code
 * IllegalArgumentException} → Jackson이 감싸 {@code HttpMessageNotReadableException} → 400(REQ-09 관례,
 * `GlobalExceptionHandler`).
 */
public enum ConditionTag {
  NORMAL("정상"),
  ACTIVE("활발"),
  FLOPPY_TAIL("거꾸리"),
  VOMITING("구토");

  private final String label;

  ConditionTag(String label) {
    this.label = label;
  }

  @JsonValue
  public String getLabel() {
    return label;
  }

  @JsonCreator
  public static ConditionTag fromLabel(String label) {
    for (ConditionTag tag : values()) {
      if (tag.label.equals(label)) {
        return tag;
      }
    }
    throw new IllegalArgumentException("Unknown condition_tag: " + label);
  }
}
