package com.petkok.business.auth.controller;

import com.petkok.business.auth.service.AuthService;
import com.petkok.data.auth.dto.KakaoLoginRequest;
import com.petkok.data.auth.dto.RefreshRequest;
import com.petkok.data.auth.dto.TokenResponse;
import com.petkok.framework.response.ApiResponse;
import com.petkok.framework.security.AuthPrincipal;
import com.petkok.framework.security.CurrentUser;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * 인증 엔드포인트.
 *
 * <p>경로 문자열을 {@code ApiUri} 상수로 빼지 않았다 — 지금 이 경로가 쓰이는 곳은 여기와 {@code SecurityConfig.PUBLIC_PATHS}
 * 뿐이고, 후자는 <b>Security 설정이 문자열을 직접 들고 있어야 의미가 있다.</b> 반복이 실제로 늘어나면 그때 상수화한다.
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

  private final AuthService authService;

  public AuthController(AuthService authService) {
    this.authService = authService;
  }

  /**
   * 카카오 로그인 / 자동가입. <b>access 토큰 없이</b> 호출된다({@code SecurityConfig.PUBLIC_PATHS}).
   *
   * <p>커스텀 플로우다 — 서버가 리다이렉트를 받지 않고, 클라이언트가 받은 인가코드를 body 로 넘겨받는다.
   */
  @PostMapping("/kakao")
  public ApiResponse<TokenResponse> loginWithKakao(@Valid @RequestBody KakaoLoginRequest request) {
    return ApiResponse.success(authService.loginWithKakao(request.code()));
  }

  /**
   * refresh 토큰 로테이션. <b>access 토큰 없이</b> 호출된다 — refresh 토큰을 body 로 받는다.
   *
   * <p>응답에는 <b>새 refresh 토큰</b>이 함께 담긴다. 클라이언트는 저장된 값을 반드시 교체해야 하며, 그러지 않고 옛 토큰을 다시 보내면 재사용 감지에 걸려
   * 전 기기가 로그아웃된다.
   */
  @PostMapping("/refresh")
  public ApiResponse<TokenResponse> refresh(@Valid @RequestBody RefreshRequest request) {
    return ApiResponse.success(authService.refresh(request.refreshToken()));
  }

  /**
   * 로그아웃. <b>인증이 필요하다</b> — {@code SecurityConfig.PUBLIC_PATHS} 에 의도적으로 빠져 있다. Request Body 가 없어
   * access 토큰이 유일한 사용자 식별 수단이기 때문이다.
   *
   * <p><b>응답 본문이 없다.</b> 204 는 정의상 본문을 갖지 않으므로 여기서만 공통 래퍼 {@code ApiResponse} 를 쓰지 않는다 — 래퍼를 씌우려면
   * 200 으로 내려야 하는데, 그건 api-list 의 "204" 계약을 바꾸는 일이다.
   */
  @DeleteMapping("/logout")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void logout(@CurrentUser AuthPrincipal principal) {
    authService.logout(principal.userId());
  }
}
