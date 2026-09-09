package com.petkok.framework.port;

import java.util.List;
import java.util.UUID;

/**
 * 다이어리 항목에 붙은 사진을 묻는 포트. 구현은 {@code business/gallery} 에 있다 (PLAN-REQ-11 결정 · Phase 0).
 *
 * <p><b>{@code UserStatusChecker}(REQ-08 D2)와 같은 형태다.</b> {@code business/diary} 가 {@code
 * business/gallery} 를 직접 참조하면 도메인 간 참조 금지({@code DomainBoundaryTest})에 걸린다 — 그래서 필요한 쪽(diary)이 필요한
 * 모양을 이 인터페이스로 선언하고, 아는 쪽(gallery)이 채운다. 의존은 여전히 {@code business → framework} 한 방향이다.
 *
 * <p>⚠️ 이 인터페이스를 {@code business/gallery} 로 옮기면 안 된다. {@code business/diary} 가 그것을 참조하는 순간 도메인 간 참조
 * 금지가 그대로 걸린다 — framework 는 도메인이 아니라서 예외 대상이다(§3).
 *
 * <p>{@code Photo} 엔티티를 돌려주지 않고 {@link PhotoSummary}·{@code int}·{@code UUID} 만 주고받는 것도 의도적이다. 엔티티를
 * 노출하면 framework 가 {@code data..entity..} 를 알게 되어 {@code FRAMEWORK_MUST_NOT_KNOW_DOMAIN} 에 걸린다.
 *
 * <p>패키지가 {@code framework.gallery} 가 아니라 {@code framework.port} 인 것도 의도적이다(2026-09-09 정정). {@code
 * gallery} 는 도메인 이름이라 framework 트리 안에 두면 도메인이 framework 로 새어든 것처럼 보인다 — {@code UserStatusChecker} 가
 * {@code framework.security}(관심사 이름)에 있는 것과 같은 이유로, 이 포트들은 도메인이 아니라 "framework가 정의하고 business가 구현하는
 * 포트"라는 패턴 자체로 묶는다.
 */
public interface PhotoLookup {

  /**
   * 해당 다이어리 항목에 첨부된 사진 수.
   *
   * @param diaryEntryId 다이어리 항목 식별자
   */
  int countByDiaryEntryId(UUID diaryEntryId);

  /**
   * 해당 다이어리 항목에 첨부된 사진 목록.
   *
   * @param diaryEntryId 다이어리 항목 식별자
   */
  List<PhotoSummary> findByDiaryEntryId(UUID diaryEntryId);
}
