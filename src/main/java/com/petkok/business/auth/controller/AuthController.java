package com.petkok.business.auth.controller;

import com.petkok.business.auth.service.AuthService;
import com.petkok.data.auth.dto.KakaoLoginRequest;
import com.petkok.data.auth.dto.TokenResponse;
import com.petkok.framework.response.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
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
}
