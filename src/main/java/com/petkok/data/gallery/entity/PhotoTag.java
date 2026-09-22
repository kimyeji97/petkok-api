package com.petkok.data.gallery.entity;

import com.petkok.data.common.entity.BaseCreatedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 사진 자유 태그 ({@code photo_tags}). {@code photoId} 는 {@code photos} 처럼 UUID 컬럼이고 {@code @ManyToOne} 이
 * 아니다 — 펫 소유권은 {@code photos} 에서 이미 검증되므로 여기 {@code pet_id} 를 중복시키지 않는다(PLAN-REQ-22 §제약·함정).
 */
@Entity
@Table(name = "photo_tags")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PhotoTag extends BaseCreatedEntity {

  @Id
  @GeneratedValue
  @Column(name = "id", nullable = false, updatable = false)
  private UUID id;

  @Column(name = "photo_id", nullable = false, updatable = false)
  private UUID photoId;

  @Column(name = "tag", nullable = false, updatable = false, length = 50)
  private String tag;

  private PhotoTag(UUID photoId, String tag) {
    this.photoId = photoId;
    this.tag = tag;
  }

  public static PhotoTag of(UUID photoId, String tag) {
    return new PhotoTag(photoId, tag);
  }
}
