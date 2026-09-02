package com.petkok.data.shed.enums;

/**
 * 탈피 예측 신뢰도. Notion {@code API I/F} 「🦎 탈피 예측」 행 — {@code LOW}(기록 1개) · {@code MEDIUM}(2개) · {@code
 * HIGH}(3개 이상). 검증 계약 REQ-10-84 ~ 91.
 */
public enum PredictionConfidence {
  LOW,
  MEDIUM,
  HIGH
}
