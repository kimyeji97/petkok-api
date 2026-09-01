package com.petkok.data.feeding.enums;

/**
 * 급여 곤충 사이즈. DB 는 {@code varchar(1)}, CHECK 없이 앱에서만 검증한다 (AGENTS §5).
 *
 * <p>🦎 게코 곤충 사이즈 표시용이지만 <b>종과 무관하게 그대로 저장한다</b> — "개/고양이 미사용"은 입력 UI 규약이지 서버 거부 규약이 아니다
 * (PLAN-REQ-10 미결 질문 Phase 3). 검증 계약 REQ-10-56 · 57.
 */
public enum FoodSize {
  S,
  M,
  L
}
