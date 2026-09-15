package com.petkok.data.timeline.enums;

/**
 * 타임라인 조회 필터. Notion {@code API I/F} 「통합 타임라인」 {@code type} 쿼리 파라미터(원본 소문자 값) — {@code
 * all|feeding|activity|weight|shed|diary}. gallery(photos)는 원본 enum에 없어 포함하지 않는다(PLAN-REQ-12 §범위 —
 * 제외).
 */
public enum TimelineType {
  ALL,
  DIARY,
  FEEDING,
  ACTIVITY,
  WEIGHT,
  SHED;

  /** 쿼리 파라미터 문자열({@code null} 이면 {@code all}) → enum. 대소문자 무관. */
  public static TimelineType fromParam(String value) {
    if (value == null) {
      return ALL;
    }
    return TimelineType.valueOf(value.toUpperCase());
  }
}
