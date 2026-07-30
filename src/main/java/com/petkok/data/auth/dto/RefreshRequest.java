package com.petkok.data.auth.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * {@code POST /api/v1/auth/refresh} 요청. access 토큰 없이 호출된다 — refresh 토큰을 <b>body 로</b> 받기 때문이다.
 *
 * <p>전역 snake_case(Jackson) 설정에 따라 JSON 키는 {@code refresh_token} 이다.
 *
 * @param refreshToken 저장해 둔 refresh 토큰 원문. 응답으로 <b>새 토큰</b>이 오므로 클라이언트는 이 값을 교체해야 한다
 */
public record RefreshRequest(@NotBlank String refreshToken) {}
