package com.petkok.data.auth.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * {@code POST /api/v1/auth/kakao} 요청. access 토큰 없이 호출된다.
 *
 * @param code 클라이언트가 카카오에서 받은 인가코드. <b>1회용이고 약 10분 만료</b>다
 */
public record KakaoLoginRequest(@NotBlank String code) {}
