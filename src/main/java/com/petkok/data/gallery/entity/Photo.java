package com.petkok.data.gallery.entity;

import com.petkok.data.common.entity.BaseCreatedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 갤러리 사진 ({@code photos}). 검증 계약 REQ-11-09 · 10 · 26 (PLAN-REQ-11 § 검증 계약).
 *
 * <p><b>{@code deleted_at} 이 없다 — 삭제는 행 삭제다.</b> {@code users}·{@code pets} 만 소프트 딜리트다(AGENTS §5).
 *
 * <p><b>{@code petId} 는 {@code UUID} 컬럼이고 {@code @ManyToOne} 이 아니다.</b> {@code Pet} 은 {@code
 * data/pet} 에 있어 연관관계를 걸면 {@code data/gallery → data/pet.entity} 참조가 되어 ArchUnit 에 걸린다. 소유권은 {@code
 * PetAccessGuard} 가 판정하므로 여기서 Pet 으로 탐색할 일이 없다.
 *
 * <p>{@code diaryEntryId} 는 {@code NULL} 이면 단독 갤러리, 값이 있으면 일기 첨부다(db-schema.md §9). Phase 1 은 이 값을
 * 그대로 저장만 한다 — 다이어리 쪽에서 읽어 가는 것(REQ-10-108~110 불변식을 뒤집는 일)은 Phase 2 다.
 */
@Entity
@Table(name = "photos")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Photo extends BaseCreatedEntity {

  @Id
  @GeneratedValue
  @Column(name = "id", nullable = false, updatable = false)
  private UUID id;

  @Column(name = "pet_id", nullable = false, updatable = false)
  private UUID petId;

  @Column(name = "diary_entry_id", updatable = false)
  private UUID diaryEntryId;

  @Column(name = "image_url", nullable = false, length = 1000, updatable = false)
  private String imageUrl;

  @Column(name = "caption", length = 500)
  private String caption;

  @Column(name = "taken_at")
  private LocalDate takenAt;

  private Photo(UUID petId, UUID diaryEntryId, String imageUrl, String caption, LocalDate takenAt) {
    this.petId = petId;
    this.diaryEntryId = diaryEntryId;
    this.imageUrl = imageUrl;
    this.caption = caption;
    this.takenAt = takenAt;
  }

  public static Photo of(
      UUID petId, UUID diaryEntryId, String imageUrl, String caption, LocalDate takenAt) {
    return new Photo(petId, diaryEntryId, imageUrl, caption, takenAt);
  }
}
