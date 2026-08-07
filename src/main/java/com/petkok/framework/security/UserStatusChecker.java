package com.petkok.framework.security;

import java.util.UUID;

/**
 * 사용자가 아직 활성인지 묻는 포트. 구현은 {@code business/user} 에 있다 (PLAN-REQ-08 D2).
 *
 * <p><b>framework 가 인터페이스를 정의하고 business 가 구현하는 첫 사례다.</b> 방향이 뒤집힌 것처럼 보이지만 아니다 — 필요한 쪽(필터)이 필요한
 * 모양을 선언하고, 아는 쪽(서비스)이 그것을 채운다. 의존은 여전히 {@code business → framework} 한 방향이다.
 *
 * <p>⚠️ <b>이 인터페이스를 {@code business/user} 로 옮기면 안 된다.</b> 필터가 그것을 참조하는 순간 구조 규칙 <b>두 개가 동시에</b> 깨진다
 * —
 *
 * <ul>
 *   <li>{@code FRAMEWORK_MUST_NOT_KNOW_DOMAIN} — framework 가 business·data 를 참조
 *   <li>{@code LAYER_DIRECTION} — 필터는 정의된 세 레이어 어디에도 속하지 않는데 {@code Repository} 레이어는 {@code
 *       mayOnlyBeAccessedByLayers("Service")} 다
 * </ul>
 *
 * <p>2026-08-03 프로브에서 실측했다 — 필터에 {@code UserRepository} 를 직접 심자 두 규칙이 <b>함께</b> 빨간불이 됐다. 즉 규칙 하나를
 * 열어도 직참조는 성립하지 않으며, <b>포트가 둘을 동시에 만족시키는 유일한 길이다.</b>
 *
 * <p>반환값만 넘기고 엔티티를 노출하지 않는 것도 의도적이다. {@code User} 를 돌려주면 framework 가 {@code data..entity..} 를 알게
 * 된다.
 */
public interface UserStatusChecker {

  /**
   * 해당 사용자가 활성 상태인지.
   *
   * <p>탈퇴한 사용자(소프트 딜리트)는 {@code false} 다. 매 인증 요청마다 DB 왕복 1회가 붙는다 — 캐시 도입 여부는 실사용 트래픽을 보고 정한다
   * (PLAN-REQ-08 미결).
   *
   * @param userId access 토큰에서 꺼낸 사용자 식별자
   */
  boolean isActive(UUID userId);
}
