package com.petkok.data.auth.dto;

/**
 * 카카오 토큰 교환 응답 ({@code POST kauth.kakao.com/oauth/token}).
 *
 * <p>⚠️ <b>{@code accessToken}·{@code refreshToken} 이 본문에 평문으로 들어온다.</b> 로깅 인터셉터가 본문을 마스킹하지 않으면 그대로
 * 로그에 남는다 (AGENTS.md §5 위반) — {@code MaskingUtil.maskingCredentialsInBody} 가 이 때문에 있다.
 *
 * <p>여기 담기는 토큰은 <b>카카오의 것</b>이며 petkok 이 발급하는 access/refresh 와 다르다. 프로필 조회에 한 번 쓰고 저장하지 않는다.
 *
 * <p>필드명은 camelCase 로 두고 전역 snake_case 설정({@code JacksonConfig})이 매핑한다.
 */
public record KakaoTokenResponse(
    String accessToken,
    String tokenType,
    String refreshToken,
    Integer expiresIn,
    String scope,
    Integer refreshTokenExpiresIn) {}
