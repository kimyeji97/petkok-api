package com.petkok.business.user.service;

import com.petkok.data.user.dto.UserResponse;
import com.petkok.data.user.dto.UserUpdateRequest;
import com.petkok.data.user.entity.User;
import com.petkok.data.user.repository.UserRepository;
import com.petkok.data.user.repository.UserSocialAccountRepository;
import com.petkok.framework.exception.BusinessException;
import com.petkok.framework.exception.ErrorCode;
import com.petkok.framework.security.UserStatusChecker;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 내 프로필 조회·수정·탈퇴. 검증 계약 REQ-08-01 ~ 05 · 09 · 10 · 12 ~ 14 (PLAN-REQ-08 § 검증 계약).
 *
 * <p>{@link UserStatusChecker} 를 구현한다 — <b>framework 가 정의한 인터페이스를 business 가 채우는 첫 사례</b>다 (D2). 의존
 * 방향은 여전히 {@code business → framework} 한 방향이다.
 *
 * <p><b>{@code data/auth} 를 참조하지 않는다 — 의도적이다</b> (D5). 탈퇴가 refresh 토큰을 revoke 하면 {@code
 * business/user → data/auth} 참조가 생겨 ArchUnit 도메인 간 참조 예외가 4→5 로 는다. {@link #withdraw} 주석 참고.
 */
@Slf4j
@Service
public class UserService implements UserStatusChecker {

  private final UserRepository userRepository;
  private final UserSocialAccountRepository socialAccountRepository;
  private final Clock clock;

  public UserService(
      UserRepository userRepository,
      UserSocialAccountRepository socialAccountRepository,
      Clock clock) {
    this.userRepository = userRepository;
    this.socialAccountRepository = socialAccountRepository;
    this.clock = clock;
  }

  /**
   * 내 프로필 조회. 검증 계약 REQ-08-02.
   *
   * <p>{@code findByIdAndDeletedAtIsNull} 을 쓴다 — 탈퇴한 계정이 살아 있는 것처럼 조회되면 안 된다. 토큰이 유효해도 그 사이 탈퇴했을 수
   * 있으므로 <b>여기서도 확인한다</b>(필터의 활성 검사는 Phase 3 이다).
   *
   * @param userId access 토큰에서 꺼낸 사용자 식별자
   */
  @Transactional(readOnly = true)
  public UserResponse getMe(UUID userId) {
    return toResponse(findActive(userId));
  }

  /**
   * 내 프로필 수정. <b>보낸 필드만 반영한다</b> (PLAN-REQ-08 D3). 닉네임은 트림 후 저장(D9). 검증 계약 REQ-08-03 ~ 05 · 27 ~
   * 29.
   *
   * <p>⚠️ <b>병합을 여기서 하는 것이 이 메서드의 핵심이다</b> (D3 · D6). {@link User#updateProfile} 은 두 필드를 <b>무조건
   * 덮어쓴다</b> — 요청 값을 그대로 넘기면 닉네임만 담긴 PATCH 가 {@code profile_image_url} 을 지운다. 응답은 200 으로 정상이고 <b>DB
   * 만 조용히 손상된다.</b>
   *
   * <p>고칠 곳은 <b>여기이지 엔티티가 아니다.</b> 엔티티가 {@code null} 을 "변경 없음"으로 해석하기 시작하면 {@code null} 에 도메인 의미가
   * 붙어, "원본에 없는 규약을 만들지 않는다"(D3)가 무너진다. 부분 반영은 HTTP PATCH 의 관심사다. {@code UserTest} 의 REQ-08-08 이
   * 엔티티 쪽 계약을 고정하고 있다.
   *
   * <p>변경 감지(dirty checking)로 반영되므로 {@code save} 를 부르지 않는다.
   */
  @Transactional
  public UserResponse updateMe(UUID userId, UserUpdateRequest request) {
    User user = findActive(userId);

    user.updateProfile(
        request.nickname() != null ? normalizeNickname(request.nickname()) : user.getNickname(),
        request.profileImageUrl() != null ? request.profileImageUrl() : user.getProfileImageUrl());

    return toResponse(user);
  }

  /**
   * 닉네임 정규화 — 앞뒤 공백 트림, 트림 후 빈 값은 거부. 검증 계약 REQ-08-27 · 28 (PLAN-REQ-08 D9).
   *
   * <p>{@code @Size(min = 1)} 은 {@code " "} 를 통과시키므로 <b>공백만인 값은 여기서만 걸린다.</b> {@code null} 은 호출부가
   * 먼저 걸러 "변경 없음"으로 처리한다(D3) — 이 메서드는 {@code null} 을 받지 않는다. 중복 검사는 하지 않는다(D9 — 스키마에 UNIQUE 없음).
   */
  private static String normalizeNickname(String raw) {
    String nickname = raw.strip();
    if (nickname.isEmpty()) {
      throw new BusinessException(ErrorCode.INVALID_INPUT);
    }
    return nickname;
  }

  /**
   * 프로필 이미지 제거 — {@code profile_image_url} 을 {@code null} 로. 검증 계약 REQ-08-22 ~ 24 (PLAN-REQ-08 D8).
   *
   * <p><b>{@code PATCH /users/me} 로는 못 하는 일이라 별도 메서드다.</b> D3 이 누락·{@code null} 을 모두 "변경 없음"으로 두므로
   * PATCH 에는 제거 신호를 실을 자리가 없다. {@code null} = 제거로 바꾸면 D3 이 뒤집히고, {@code ""} = 제거는 원본에 없는 규약이다.
   *
   * <p>{@link User#updateProfile} 은 두 필드를 무조건 덮어쓰므로 <b>닉네임을 채워 넘긴다</b>(D6 과 같은 이유 — 빠뜨리면 닉네임이
   * {@code null} 이 되어 {@code NOT NULL} 위반으로 500). 이미 이미지가 없어도 예외 없이 끝난다(멱등).
   */
  @Transactional
  public void removeProfileImage(UUID userId) {
    User user = findActive(userId);
    user.updateProfile(user.getNickname(), null);
  }

  /**
   * 회원 탈퇴. {@code users.deleted_at} 을 찍고 소셜 연결을 <b>하드 삭제</b>한다. 검증 계약 REQ-08-09 · 10 · 13 · 14.
   *
   * <p><b>소셜 행을 먼저 지운다.</b> FK 는 {@code user_social_accounts.user_id → users.id} 한 방향이라 순서를 뒤집어도 제약
   * 위반은 없지만, 뒤집을 이유도 없다.
   *
   * <p>⚠️ <b>refresh 토큰을 revoke 하지 않는다 — 빠뜨린 것이 아니라 결정이다</b> (PLAN-REQ-08 D5). revoke 하려면 {@code
   * data/auth} 를 참조해야 하고 그러면 ArchUnit 도메인 간 참조 예외가 <b>4→5 로 는다.</b> 탈퇴 계정은 필터의 활성 검사(Phase 3)가 어떤
   * access 토큰을 들고 와도 막으므로, refresh 로 새 토큰을 받아도 결국 차단된다.
   *
   * <p>대가 둘 — {@code refresh_tokens} 에 {@code revoked_at IS NULL} 행이 남고, <b>탈퇴한 사용자도 {@code
   * /auth/refresh} 가 200 과 새 토큰을 반환한다</b>(그 토큰으로 API 를 부르면 401). 후자는 계획서 미결이다.
   *
   * <p>⚠️ <b>여기서 예외를 던지면 두 쓰기가 전부 사라진다</b> (AGENTS §5). 지금은 남겨야 할 쓰기가 없어 기본 롤백이 맞지만, "실패해도 남겨야 하는"
   * 기록이 생기면 {@code noRollbackFor} 를 명시해야 한다 — 2026-07-30 {@code AuthService.refresh} 에서 같은 함정이 실제로
   * 터졌고 <b>목 기반 테스트는 통과했다.</b>
   *
   * @param userId access 토큰에서 꺼낸 사용자 식별자
   */
  @Transactional
  public void withdraw(UUID userId) {
    User user = findActive(userId);

    socialAccountRepository.deleteByUserId(userId);
    user.softDelete(OffsetDateTime.now(clock));

    log.info("User withdrawn. userId={}", userId);
  }

  /**
   * {@link UserStatusChecker} 구현. 필터가 매 인증 요청마다 부른다. 검증 계약 REQ-08-16 · 17.
   *
   * <p><b>구현체를 따로 만들지 않고 이 서비스가 직접 구현한다</b> (D2) — 새로 느는 것은 인터페이스 1개뿐이다.
   *
   * <p>탈퇴 즉시 차단이 목적이다. 이게 없으면 탈퇴해도 기존 access 토큰이 <b>최대 30분</b> 살아 있다({@code JWT_ACCESS_TTL}) — 필터가
   * 서명·타입만 보고 DB 를 안 보기 때문이다.
   *
   * <p>예외를 던지지 않고 {@code boolean} 을 돌려주는 것도 계약의 일부다. 필터는 이 값을 보고 <b>인증을 세팅하지 않을 뿐</b>이며, 거절은 {@code
   * SecurityConfig} 의 entryPoint 가 한다.
   */
  @Override
  @Transactional(readOnly = true)
  public boolean isActive(UUID userId) {
    return userRepository.findByIdAndDeletedAtIsNull(userId).isPresent();
  }

  private User findActive(UUID userId) {
    return userRepository
        .findByIdAndDeletedAtIsNull(userId)
        .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
  }

  private static UserResponse toResponse(User user) {
    return new UserResponse(
        user.getId(),
        user.getNickname(),
        user.getEmail(),
        user.getProfileImageUrl(),
        user.getCreatedAt());
  }
}
