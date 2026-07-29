package com.petkok.data.user.enums;

/**
 * 소셜 로그인 provider.
 *
 * <p>DB 컬럼은 varchar(20) 이고 CHECK 제약이 없다 — 검증은 앱 레이어에서만 한다(AGENTS.md §5). 신규 provider 추가 시 {@code
 * ALTER TABLE} 없이 앱 배포만으로 처리하기 위함이다.
 *
 * <p>{@code KAKAO} 외 둘은 확장 예정이며 REQ-07 범위가 아니다.
 */
public enum SocialProvider {
  KAKAO,
  GOOGLE,
  APPLE
}
