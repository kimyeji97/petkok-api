package com.petkok.business.user.controller;

import com.petkok.business.user.service.UserService;
import com.petkok.data.user.dto.UserResponse;
import com.petkok.data.user.dto.UserUpdateRequest;
import com.petkok.framework.response.ApiResponse;
import com.petkok.framework.security.AuthPrincipal;
import com.petkok.framework.security.CurrentUser;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * 사용자 엔드포인트. <b>전부 인증이 필요하다</b> — {@code /users/**} 는 {@code SecurityConfig.PUBLIC_PATHS} 에 없다.
 *
 * <p>경로 문자열을 {@code ApiUri} 상수로 빼지 않은 이유는 {@code AuthController} 와 같다 — 반복이 실제로 늘어나면 그때 상수화한다.
 */
@RestController
@RequestMapping("/api/v1/users")
public class UserController {

  private final UserService userService;

  public UserController(UserService userService) {
    this.userService = userService;
  }

  /** 내 프로필 조회. 응답 필드는 5개다({@link UserResponse}). */
  @GetMapping("/me")
  public ApiResponse<UserResponse> getMe(@CurrentUser AuthPrincipal principal) {
    return ApiResponse.success(userService.getMe(principal.userId()));
  }

  /**
   * 내 프로필 수정. <b>보낸 필드만 반영된다</b> — 누락과 {@code null} 은 둘 다 "변경 없음"이다({@link UserUpdateRequest}).
   *
   * <p>{@code PUT} 이 아니라 {@code PATCH} 인 것은 AGENTS.md §5 규약이다.
   */
  @PatchMapping("/me")
  public ApiResponse<UserResponse> updateMe(
      @CurrentUser AuthPrincipal principal, @Valid @RequestBody UserUpdateRequest request) {
    return ApiResponse.success(userService.updateMe(principal.userId(), request));
  }

  /**
   * 프로필 이미지 제거. 검증 계약 REQ-08-21 (PLAN-REQ-08 D8).
   *
   * <p>{@code PATCH /users/me} 와 분리한 이유는 {@link UserUpdateRequest} 참고 — {@code null} 이 "변경 없음"이라
   * 제거를 표현할 수 없다. 204 · 본문 없음은 {@link #withdraw} 와 같은 이유로 {@code ApiResponse} 를 쓰지 않는다.
   */
  @DeleteMapping("/me/profile-image")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void removeProfileImage(@CurrentUser AuthPrincipal principal) {
    userService.removeProfileImage(principal.userId());
  }

  /**
   * 회원 탈퇴 (소프트 딜리트). 검증 계약 REQ-08-15.
   *
   * <p><b>응답 본문이 없다.</b> 204 는 정의상 본문을 갖지 않으므로 공통 래퍼 {@code ApiResponse} 를 쓰지 않는다 — 씌우려면 200 으로 내려야
   * 하는데 그건 Notion {@code API I/F} 의 "Response 204: 응답 바디 없음" 계약을 바꾸는 일이다. {@code
   * AuthController.logout} 과 같은 이유다.
   */
  @DeleteMapping("/me")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void withdraw(@CurrentUser AuthPrincipal principal) {
    userService.withdraw(principal.userId());
  }
}
