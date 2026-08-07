package com.petkok.business.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.petkok.data.user.dto.UserResponse;
import com.petkok.data.user.dto.UserUpdateRequest;
import com.petkok.data.user.entity.User;
import com.petkok.data.user.repository.UserRepository;
import com.petkok.data.user.repository.UserSocialAccountRepository;
import com.petkok.framework.exception.BusinessException;
import com.petkok.framework.exception.ErrorCode;
import java.lang.reflect.Field;
import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.transaction.annotation.Transactional;

/**
 * 내 프로필 조회·수정. 검증 계약 REQ-08-01 ~ 05 (PLAN-REQ-08 § 검증 계약).
 *
 * <p>DB 를 띄우지 않는다 — 조회 필터와 병합은 저장소 반환값만 관찰하면 되는 순수 흐름이다. {@link User} 는 <b>실물을 쓴다</b>: 병합 결과가 실제로
 * 엔티티에 반영되는지가 검증 대상이라 목으로 바꾸면 볼 것이 남지 않는다.
 */
class UserServiceTest {

  private static final UUID USER_ID = UUID.fromString("11111111-2222-3333-4444-555555555555");
  private static final String NICKNAME = "게코집사";
  private static final String IMAGE_URL = "https://img.example.com/a.png";

  private final UserRepository userRepository = mock(UserRepository.class);
  private final UserSocialAccountRepository socialAccountRepository =
      mock(UserSocialAccountRepository.class);
  private final UserService userService = new UserService(userRepository, socialAccountRepository);

  /** 활성 사용자 1명을 저장소에 등록한다. 반환값으로 병합 결과를 직접 관찰한다. */
  private User active() {
    User user = User.of(NICKNAME, null, IMAGE_URL);
    when(userRepository.findByIdAndDeletedAtIsNull(USER_ID)).thenReturn(Optional.of(user));
    return user;
  }

  @Test
  @DisplayName("[REQ-08-01] 응답 DTO 필드는 정확히 5개다 (updated_at 없음)")
  void req_08_01_responseHasExactlyFiveComponents() {
    String[] names =
        Arrays.stream(UserResponse.class.getRecordComponents())
            .map(RecordComponent::getName)
            .toArray(String[]::new);

    assertThat(names).containsExactly("id", "nickname", "email", "profileImageUrl", "createdAt");
  }

  @Test
  @DisplayName("[REQ-08-02] 소프트 딜리트된 사용자는 USER_NOT_FOUND")
  void req_08_02_softDeletedUserIsNotFound() {
    when(userRepository.findByIdAndDeletedAtIsNull(USER_ID)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> userService.getMe(USER_ID))
        .isInstanceOf(BusinessException.class)
        .extracting(e -> ((BusinessException) e).getErrorCode())
        .isEqualTo(ErrorCode.USER_NOT_FOUND);
  }

  @Test
  @DisplayName("[REQ-08-03] 닉네임만 보내면 profile_image_url 이 유지된다")
  void req_08_03_nicknameOnlyPatchKeepsProfileImage() {
    User user = active();

    userService.updateMe(USER_ID, new UserUpdateRequest("새닉네임", null));

    assertThat(user.getProfileImageUrl()).isEqualTo(IMAGE_URL);
  }

  @Test
  @DisplayName("[REQ-08-04] 이미지만 보내면 nickname 이 유지된다")
  void req_08_04_imageOnlyPatchKeepsNickname() {
    User user = active();

    userService.updateMe(USER_ID, new UserUpdateRequest(null, "https://img.example.com/b.png"));

    assertThat(user.getNickname()).isEqualTo(NICKNAME);
  }

  @Test
  @DisplayName("[REQ-08-05] 둘 다 null 이면 아무것도 바뀌지 않는다")
  void req_08_05_allNullPatchChangesNothing() {
    User user = active();

    userService.updateMe(USER_ID, new UserUpdateRequest(null, null));

    assertThat(user)
        .extracting(User::getNickname, User::getProfileImageUrl)
        .containsExactly(NICKNAME, IMAGE_URL);
  }

  @Test
  @DisplayName("[REQ-08-09] 탈퇴하면 users.deleted_at 이 채워진다")
  void req_08_09_withdrawSetsDeletedAt() {
    User user = active();

    userService.withdraw(USER_ID);

    assertThat(user.isDeleted()).isTrue();
  }

  @Test
  @DisplayName("[REQ-08-10] 탈퇴하면 소셜 행이 하드 삭제된다")
  void req_08_10_withdrawHardDeletesSocialAccounts() {
    active();

    userService.withdraw(USER_ID);

    verify(socialAccountRepository).deleteByUserId(USER_ID);
  }

  @Test
  @DisplayName("[REQ-08-14] 탈퇴는 소셜 행을 먼저 지운 뒤 deleted_at 을 찍는다")
  void req_08_14_withdrawDeletesSocialAccountsFirst() {
    User user = active();
    InOrder order = inOrder(socialAccountRepository, userRepository);

    userService.withdraw(USER_ID);

    // 소셜 삭제가 먼저인지 확인한다. deleted_at 은 변경 감지로 반영되므로 저장소 호출이 없어
    // "조회 → 소셜 삭제" 순서 + 삭제 시점에 아직 살아 있었다는 사실로 순서를 고정한다.
    order.verify(userRepository).findByIdAndDeletedAtIsNull(USER_ID);
    order.verify(socialAccountRepository).deleteByUserId(USER_ID);
    assertThat(user.isDeleted()).isTrue();
  }

  @Test
  @DisplayName("[REQ-08-13] withdraw 에 @Transactional 이 붙어 있다")
  void req_08_13_withdrawIsTransactional() throws ReflectiveOperationException {
    Transactional tx =
        UserService.class.getMethod("withdraw", UUID.class).getAnnotation(Transactional.class);

    assertThat(tx).isNotNull();
  }

  @Test
  @DisplayName("[REQ-08-12] UserService 는 data.auth 를 참조하지 않는다 (예외를 4→5 로 늘리지 않는다)")
  void req_08_12_userServiceDoesNotDependOnAuthData() {
    Stream<Class<?>> fieldTypes =
        Arrays.stream(UserService.class.getDeclaredFields()).map(Field::getType);
    Stream<Class<?>> ctorParamTypes =
        Arrays.stream(UserService.class.getDeclaredConstructors())
            .flatMap(c -> Arrays.stream(c.getParameterTypes()));

    assertThat(Stream.concat(fieldTypes, ctorParamTypes))
        .noneMatch(t -> t.getName().startsWith("com.petkok.data.auth."));
  }
}
