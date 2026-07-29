package com.petkok.data.auth.dto;

/**
 * 로그인·재발급 응답. <b>petkok 이 발급한</b> 토큰이며 카카오 토큰이 아니다.
 *
 * <p>{@code /auth/refresh} 는 로테이션이라 <b>새 refresh 토큰을 함께</b> 돌려준다 — 클라이언트는 저장된 값을 반드시 교체해야 한다.
 *
 * <p>응답 필드는 전역 snake_case 로 나간다({@code access_token}, {@code refresh_token}).
 */
public record TokenResponse(String accessToken, String refreshToken) {}
