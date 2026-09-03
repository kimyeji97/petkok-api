package com.petkok.data.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import java.time.OffsetDateTime;

/**
 * created_at + updated_at + deleted_at (소프트 딜리트) 베이스. (users, pets) 삭제 = deleted_at 설정, 활성 조회 =
 * deleted_at IS NULL.
 */
@MappedSuperclass
public abstract class BaseSoftDeleteEntity extends BaseTimeEntity {

  @Column(name = "deleted_at")
  private OffsetDateTime deletedAt;

  public OffsetDateTime getDeletedAt() {
    return deletedAt;
  }

  public boolean isDeleted() {
    return deletedAt != null;
  }

  /**
   * 소프트 딜리트 — 호출부가 {@code Clock} 으로 구한 순간을 넘긴다. {@code RefreshToken#revoke(OffsetDateTime)} 와 같은
   * 형태다(REQ-16 미결 ⑦, 2026-09-03 확정) — 무인자 {@code now()} 는 JPA 엔티티라 빈을 주입할 수 없어 REQ-16-10 규칙
   * 범위(business·framework) 밖이지만, 호출부가 이미 주입받은 {@code Clock} 을 갖고 있으므로 파라미터로 받지 않을 이유가 없다.
   */
  public void softDelete(OffsetDateTime now) {
    this.deletedAt = now;
  }
}
