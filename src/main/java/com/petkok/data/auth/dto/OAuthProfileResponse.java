package com.petkok.data.auth.dto;

/**
 * provider 응답을 petkok 이 쓰는 형태로 정규화한 프로필. OAuth 클라이언트가 외부 응답을 이 형태로 바꿔 돌려준다.
 *
 * <p>provider 별 응답 구조(카카오의 {@code kakao_account.profile} 중첩 등)를 여기서 흡수하므로, 서비스는 provider 를 몰라도 된다.
 * Google·Apple 을 붙일 때도 이 형태를 맞추면 된다.
 *
 * @param providerUserId provider 가 발급한 사용자 ID. 카카오는 Long 이라 문자열로 변환해 담는다
 * @param nickname {@code users.nickname} 이 NOT NULL 이라 반드시 있어야 한다
 * @param email <b>카카오는 항상 {@code null}</b> (2026-07-29 실측)
 * @param profileImageUrl <b>{@code https} 로 정규화된</b> URL. 원본은 {@code http} 로 온다
 */
public record OAuthProfileResponse(
    String providerUserId, String nickname, String email, String profileImageUrl) {}
