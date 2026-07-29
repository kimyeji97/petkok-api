package com.petkok.data.user.entity;

import com.petkok.data.common.entity.BaseSoftDeleteEntity;
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
 * 서비스 사용자. 소셜 로그인 인증 정보는 {@link UserSocialAccount} 로 분리한다.
 *
 * <p><b>{@code email} 은 비어 있는 것이 정상이다.</b> Kakao 는 이메일을 내려주지 않는다(2026-07-29 실측 {@code
 * kakao_account.email = null}) — 비즈니스 앱 전환 + 검수가 필요한 동의항목이라 현재 앱에서는 받을 수 없다. 따라서 사용자 식별자는 {@code
 * (provider, provider_user_id)} 하나뿐이고, 이메일을 전제로 한 계정 병합·중복 검사는 만들 수 없다.
 */
@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseSoftDeleteEntity {

  /**
   * ID 는 Hibernate 가 채운다. 앱 코드가 직접 채우면 Spring Data 가 "ID 가 있으니 기존 엔티티"로 보고 {@code save()} 를 merge 로
   * 처리해 INSERT 전에 SELECT 를 한 번 더 날린다.
   */
  @Id
  @GeneratedValue
  @Column(name = "id", nullable = false, updatable = false)
  private UUID id;

  @Column(name = "nickname", nullable = false, length = 100)
  private String nickname;

  @Column(name = "email", length = 255)
  private String email;

  @Column(name = "profile_image_url", length = 500)
  private String profileImageUrl;

  private User(String nickname, String email, String profileImageUrl) {
    this.nickname = nickname;
    this.email = email;
    this.profileImageUrl = profileImageUrl;
  }

  /**
   * 소셜 자동가입용 생성.
   *
   * @param nickname provider 프로필의 닉네임. NOT NULL 이므로 Kakao 는 {@code profile_nickname} 동의항목이 반드시 켜져
   *     있어야 한다
   * @param email provider 가 주지 않으면 {@code null} (Kakao 는 항상 {@code null})
   * @param profileImageUrl provider 프로필 이미지. 없으면 {@code null}
   */
  public static User of(String nickname, String email, String profileImageUrl) {
    return new User(nickname, email, profileImageUrl);
  }

  public void updateProfile(String nickname, String profileImageUrl) {
    this.nickname = nickname;
    this.profileImageUrl = profileImageUrl;
  }
}
