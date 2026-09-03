package com.petkok.data.diary.entity;

import com.petkok.data.common.entity.BaseTimeEntity;
import com.petkok.data.diary.enums.ConditionTag;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 다이어리 항목 ({@code diary_entries}). {@code BaseTimeEntity} 상속(D11) — {@code updated_at} 이 있다, 나머지 4
 * 기록 도메인과 다르다. {@code deleted_at} 없음(D7) · {@code petId} 는 UUID 컬럼({@code @ManyToOne} 아님).
 *
 * <p>사진 연결(`photo_ids`·`photos`·`photo_count`)은 REQ-11 로 이관됐다(D4) — 이 엔티티엔 그 필드가 아예 없다.
 */
@Entity
@Table(name = "diary_entries")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DiaryEntry extends BaseTimeEntity {

  @Id
  @GeneratedValue
  @Column(name = "id", nullable = false, updatable = false)
  private UUID id;

  @Column(name = "pet_id", nullable = false, updatable = false)
  private UUID petId;

  @Column(name = "title", length = 200)
  private String title;

  @Column(name = "content", columnDefinition = "text")
  private String content;

  @Enumerated(EnumType.STRING)
  @Column(name = "condition_tag", length = 50)
  private ConditionTag conditionTag;

  @Column(name = "entry_date", nullable = false)
  private LocalDate entryDate;

  private DiaryEntry(
      UUID petId, String title, String content, ConditionTag conditionTag, LocalDate entryDate) {
    this.petId = petId;
    this.title = title;
    this.content = content;
    this.conditionTag = conditionTag;
    this.entryDate = entryDate;
  }

  public static DiaryEntry of(
      UUID petId, String title, String content, ConditionTag conditionTag, LocalDate entryDate) {
    return new DiaryEntry(petId, title, content, conditionTag, entryDate);
  }

  /**
   * 수정. <b>받은 값을 그대로 쓴다</b> — {@code null} 에 "변경 없음" 의미를 두지 않는다 (AGENTS §5). 부분 반영 병합은 {@code
   * DiaryService} 가 한다.
   */
  public void update(String title, String content, ConditionTag conditionTag, LocalDate entryDate) {
    this.title = title;
    this.content = content;
    this.conditionTag = conditionTag;
    this.entryDate = entryDate;
  }
}
