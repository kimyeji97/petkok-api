package com.petkok.data.user.entity;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * {@link User} 의 프로필 수정 계약. 검증 계약 REQ-08-08 (PLAN-REQ-08 § 검증 계약).
 *
 * <p><b>이 테스트가 고정하는 것은 "엔티티는 부분 반영을 모른다"는 사실이다</b> (PLAN-REQ-08 D6). {@code updateProfile} 은 받은 값을
 * 그대로 쓰며, {@code null} 을 "변경 없음"으로 해석하지 <b>않는다</b>. PATCH 의 부분 반영은 HTTP 의 관심사이므로 {@code
 * UserService} 가 병합한 뒤 넘긴다.
 *
 * <p>⚠️ <b>깨지는 방향이 중요하다.</b> 누군가 "PATCH 가 이미지를 지운다"는 증상을 보고 <b>엔티티</b>를 {@code null} 이면 유지하도록 고치면 이
 * 테스트가 빨간불이 된다 — 그게 의도다. 고칠 곳은 서비스의 병합이지 엔티티가 아니다. 반대로 엔티티를 고쳐 버리면 {@code null} 에 도메인 의미가 붙어 D3(원본에
 * 없는 규약을 만들지 않는다)이 무너진다.
 */
class UserTest {

  private static final String NICKNAME = "게코집사";
  private static final String IMAGE_URL = "https://img.example.com/a.png";

  @Test
  @DisplayName("[REQ-08-08] updateProfile 은 두 필드를 통째로 덮어쓴다 (null 을 '유지'로 읽지 않는다)")
  void req_08_08_updateProfileOverwritesBothFields() {
    User user = User.of(NICKNAME, null, IMAGE_URL);

    user.updateProfile("새닉네임", null);

    assertThat(user.getProfileImageUrl()).isNull();
  }

  @Test
  @DisplayName("[REQ-08-08] updateProfile 은 넘긴 값을 그대로 반영한다")
  void req_08_08_updateProfileAppliesGivenValues() {
    User user = User.of(NICKNAME, null, IMAGE_URL);

    user.updateProfile("새닉네임", "https://img.example.com/b.png");

    assertThat(user.getNickname()).isEqualTo("새닉네임");
  }
}
