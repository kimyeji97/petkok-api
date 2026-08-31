package com.petkok.data.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import java.time.OffsetDateTime;
import org.springframework.data.annotation.LastModifiedDate;

/** created_at + updated_at 을 갖는 엔티티용 베이스. (diary_entries 등) updated_at 은 JPA Auditing 이 관리한다. */
@MappedSuperclass
public abstract class BaseTimeEntity extends BaseCreatedEntity {

  @LastModifiedDate
  @Column(name = "updated_at", nullable = false)
  private OffsetDateTime updatedAt;

  public OffsetDateTime getUpdatedAt() {
    return updatedAt;
  }
}
