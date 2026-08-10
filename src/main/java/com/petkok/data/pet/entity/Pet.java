package com.petkok.data.pet.entity;

import com.petkok.data.common.entity.BaseSoftDeleteEntity;
import com.petkok.data.pet.enums.Gender;
import com.petkok.data.pet.enums.Species;
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
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 반려동물. 소유자는 {@code user_id} 로 참조한다.
 *
 * <p>⚠️ <b>{@code user_id} 를 {@code @ManyToOne} 이 아니라 생 {@code UUID} 컬럼으로 매핑한다.</b> {@code Pet} 은
 * {@code data/pet}, {@code User} 는 {@code data/user} 라 연관관계를 걸면 ArchUnit 도메인 간 참조 금지에 걸린다 — {@code
 * RefreshToken.userId} 와 같은 이유다(Notion 테이블 정의서 §10).
 *
 * <p>⚠️ <b>{@code @SQLDelete}·{@code @SQLRestriction} 을 쓰지 않는다</b> (PLAN-REQ-09 D6). 소프트 딜리트는
 * {@code users} 와 같이 <b>파생 쿼리로 명시 필터</b>한다 — 자동 필터는 조용하고, 네이티브 쿼리·조인에서 안 걸려 삭제된 행이 에러 없이 샌다. 검증 계약
 * REQ-09-18 이 이 부재를 고정한다.
 *
 * <p><b>{@code species} 는 등록 후 바뀌지 않는다.</b> 그래서 {@link #updateProfile} 이 받지 않는다.
 *
 * <p>⚠️ <b>{@code @Builder} 로 {@code id} 를 세팅하지 말 것.</b> 필드가 8개라 팩토리 메서드로는 Checkstyle {@code
 * ParameterNumber}(최대 7)를 만족할 수 없어 {@code @Builder} + {@code @AllArgsConstructor} 를 골랐는데, 그 부작용으로
 * <b>빌더에 {@code id} 가 노출된다.</b> 채우면 Spring Data 가 "ID 가 있으니 기존 엔티티"로 보고 {@code save()} 를 merge 로
 * 처리해 INSERT 전에 SELECT 를 한 번 더 날린다({@code User} 에도 같은 주의가 있다). <b>ID 는 Hibernate 가 채운다.</b>
 */
@Entity
@Table(name = "pets")
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Pet extends BaseSoftDeleteEntity {

  @Id
  @GeneratedValue
  @Column(name = "id", nullable = false, updatable = false)
  private UUID id;

  @Column(name = "user_id", nullable = false, updatable = false)
  private UUID userId;

  @Column(name = "name", nullable = false, length = 100)
  private String name;

  @Enumerated(EnumType.STRING)
  @Column(name = "species", nullable = false, length = 50, updatable = false)
  private Species species;

  @Column(name = "breed", length = 100)
  private String breed;

  @Enumerated(EnumType.STRING)
  @Column(name = "gender", length = 20)
  private Gender gender;

  @Column(name = "birthday")
  private LocalDate birthday;

  @Column(name = "adoption_date")
  private LocalDate adoptionDate;

  @Column(name = "profile_image_url", length = 500)
  private String profileImageUrl;

  /**
   * 프로필 수정. <b>받은 값을 그대로 쓴다</b> — {@code null} 에 "변경 없음" 같은 도메인 의미를 두지 않는다 (AGENTS §5). 부분 반영 병합은
   * {@code PetService} 가 한다.
   *
   * <p>{@code species} 가 파라미터에 없는 것이 계약이다 — 등록 후 변경 불가(D5).
   */
  public void updateProfile(
      String name,
      String breed,
      Gender gender,
      LocalDate birthday,
      LocalDate adoptionDate,
      String profileImageUrl) {
    this.name = name;
    this.breed = breed;
    this.gender = gender;
    this.birthday = birthday;
    this.adoptionDate = adoptionDate;
    this.profileImageUrl = profileImageUrl;
  }

  public boolean isOwnedBy(UUID candidateUserId) {
    return this.userId.equals(candidateUserId);
  }
}
