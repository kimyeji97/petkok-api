package com.petkok.data.user.entity;

import com.petkok.data.common.entity.BaseCreatedEntity;
import com.petkok.data.user.enums.SocialProvider;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 소셜 로그인 인증 정보. 동일 {@code user_id} 에 여러 provider 를 연결할 수 있다(통합 계정).
 *
 * <p>{@code (provider, provider_user_id)} UNIQUE 가 중복 가입을 막는다. 이 조합이 <b>유일한 사용자 식별자</b>다 — {@link
 * User#getEmail()} 은 Kakao 에서 항상 비어 있다.
 *
 * <p>쓰는 쪽은 auth 도메인이지만 엔티티는 {@code data/user} 에 둔다. {@link User} 를 {@code @ManyToOne} 으로 참조하므로
 * {@code data/auth} 에 두면 ArchUnit 도메인 간 참조 금지에 걸린다.
 */
@Entity
@Table(
    name = "user_social_accounts",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uq_social_provider",
            columnNames = {"provider", "provider_user_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserSocialAccount extends BaseCreatedEntity {

  @Id
  @GeneratedValue
  @Column(name = "id", nullable = false, updatable = false)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  /** DB 는 varchar(20). CHECK 제약 없이 앱 레이어에서만 검증한다 (AGENTS.md §5). */
  @Enumerated(EnumType.STRING)
  @Column(name = "provider", nullable = false, length = 20)
  private SocialProvider provider;

  @Column(name = "provider_user_id", nullable = false, length = 255)
  private String providerUserId;

  private UserSocialAccount(User user, SocialProvider provider, String providerUserId) {
    this.user = user;
    this.provider = provider;
    this.providerUserId = providerUserId;
  }

  /**
   * @param providerUserId provider 가 발급한 사용자 ID. Kakao 는 {@code id} 가 Long 이라 문자열로 담는다
   */
  public static UserSocialAccount of(User user, SocialProvider provider, String providerUserId) {
    return new UserSocialAccount(user, provider, providerUserId);
  }
}
