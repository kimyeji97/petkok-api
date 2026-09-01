package com.petkok.data.feeding.enums;

/**
 * 거식 스트릭 위험도. Notion {@code API I/F} 「🦎 거식 스트릭 조회」 행 — {@code NONE} · {@code CAUTION}(3일+)
 * · {@code DANGER}(7일+). 검증 계약 REQ-10-59 ~ 65.
 */
public enum StreakLevel {
  NONE,
  CAUTION,
  DANGER
}
