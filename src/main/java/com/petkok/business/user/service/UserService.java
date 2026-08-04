package com.petkok.business.user.service;

import com.petkok.data.user.dto.UserResponse;
import com.petkok.data.user.dto.UserUpdateRequest;
import com.petkok.data.user.entity.User;
import com.petkok.data.user.repository.UserRepository;
import com.petkok.framework.exception.BusinessException;
import com.petkok.framework.exception.ErrorCode;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 내 프로필 조회·수정. 검증 계약 REQ-08-01 ~ 05 (PLAN-REQ-08 § 검증 계약). */
@Slf4j
@Service
public class UserService {

  private final UserRepository userRepository;

  public UserService(UserRepository userRepository) {
    this.userRepository = userRepository;
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
   * 내 프로필 수정. <b>보낸 필드만 반영한다</b> (PLAN-REQ-08 D3). 검증 계약 REQ-08-03 ~ 05.
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
        request.nickname() != null ? request.nickname() : user.getNickname(),
        request.profileImageUrl() != null ? request.profileImageUrl() : user.getProfileImageUrl());

    return toResponse(user);
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
