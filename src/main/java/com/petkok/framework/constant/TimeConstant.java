package com.petkok.framework.constant;

import java.time.ZoneId;

/**
 * 시각 규약 상수 (REQ-16 · ADR-0002). 저장은 순간({@code timestamptz}), <b>노출·계산은 KST 고정</b>.
 *
 * <p>⚠️ <b>{@code Asia/Seoul} 문자열이 살 수 있는 유일한 자리다.</b> 2026-08-31 이전에는 세 곳에 흩어져 있었고({@code
 * JacksonConfig} · {@code OffsetDateTimeDeserializer} · {@code LocalDateTimeUtil}), 계획서는 그중 둘만 세고
 * 있었다 — <b>한 곳만 바꾸면 응답 표기와 요청 해석이 조용히 갈린다.</b> 검증 계약 REQ-16-16 이 이 규칙을 빨간불로 강제한다.
 *
 * <p>이 존이 실제로 갈리는 곳은 <b>벽시계 파생</b> 하나뿐이다 — {@code LocalDate.now(clock)} · {@code
 * LocalDateTime.now(clock)}. 저장되는 순간은 존과 무관하게 같다. 그 자리가 정확히 D4(달력 판정 = KST)이고, UTC 로 두면 KST
 * 00:00~09:00 에 "어제"가 <b>에러 없이</b> 나온다.
 */
public final class TimeConstant {

  /** 노출·계산의 기준 타임존. {@code Clock} 빈과 Jackson 렌더가 모두 이 값을 쓴다. */
  public static final ZoneId KST = ZoneId.of("Asia/Seoul");

  private TimeConstant() {}
}
