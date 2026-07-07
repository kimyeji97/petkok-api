package com.petkok.global.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import java.time.LocalDateTime;

/**
 * created_at + updated_at + deleted_at (소프트 딜리트) 베이스. (users, pets) 삭제 = deleted_at 설정, 활성 조회 =
 * deleted_at IS NULL.
 */
@MappedSuperclass
public abstract class BaseSoftDeleteEntity extends BaseTimeEntity {

  @Column(name = "deleted_at")
  private LocalDateTime deletedAt;

  public LocalDateTime getDeletedAt() {
    return deletedAt;
  }

  public boolean isDeleted() {
    return deletedAt != null;
  }

  public void softDelete() {
    this.deletedAt = LocalDateTime.now();
  }
}
