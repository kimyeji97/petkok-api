package com.petkok.data.activity.enums;

import com.petkok.data.pet.enums.Species;

/**
 * 활동 유형. DB 는 {@code varchar(50)}, CHECK 없이 앱에서만 검증한다 (AGENTS §5).
 *
 * <p><b>종별 허용값</b> (Notion {@code API I/F} 활동 기록 · 「소스 구조」 §8) — 🦎 게코 = {@code HANDLING} 만, 개/고양이
 * = {@code WALK / PLAY / GROOMING / TRAINING}. 검증은 {@code ActivityService} 가 진입 시 한다(REQ-09 D4). 검증
 * 계약 REQ-10-24 ~ 29.
 */
public enum ActivityType {
  WALK,
  PLAY,
  GROOMING,
  TRAINING,
  HANDLING;

  public boolean isAllowedFor(Species species) {
    return species == Species.CRESTED_GECKO ? this == HANDLING : this != HANDLING;
  }
}
