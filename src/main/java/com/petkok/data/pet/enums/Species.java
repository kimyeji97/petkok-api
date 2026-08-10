package com.petkok.data.pet.enums;

/**
 * 반려동물 종. DB 는 {@code varchar(50)} 이고 CHECK 제약 없이 앱 레이어에서만 검증한다 (AGENTS.md §5).
 *
 * <p><b>등록 후 변경할 수 없다</b> — Notion {@code API I/F} 가 규정한다. 그래서 {@code PetUpdateRequest} 에는 이 필드가 없다
 * (PLAN-REQ-09 D5).
 */
public enum Species {
  CRESTED_GECKO,
  DOG,
  CAT
}
