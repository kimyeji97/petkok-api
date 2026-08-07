package com.petkok.data.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 카카오 토큰 교환 응답 ({@code POST kauth.kakao.com/oauth/token}).
 *
 * <p>⚠️ <b>{@code accessToken}·{@code refreshToken} 이 본문에 평문으로 들어온다.</b> 로깅 인터셉터가 본문을 마스킹하지 않으면 그대로
 * 로그에 남는다 (AGENTS.md §5 위반) — {@code MaskingUtil.maskingCredentialsInBody} 가 이 때문에 있다.
 *
 * <p>여기 담기는 토큰은 <b>카카오의 것</b>이며 petkok 이 발급하는 access/refresh 와 다르다. 프로필 조회에 한 번 쓰고 저장하지 않는다.
 *
 * <p>⚠️ <b>{@code @JsonProperty} 로 이름을 못박는다 — 전역 snake_case 설정에 기대지 않는다.</b> {@code JacksonConfig}
 * 의 {@code Jackson2ObjectMapperBuilderCustomizer} 는 <b>Spring Boot 가 자동 구성하는 ObjectMapper 에만</b>
 * 적용되는데, 이 DTO 를 읽는 것은 {@code RestTemplateConfig} 가 {@code new RestTemplate()} 으로 만든 인스턴스이고 그건
 * <b>자기 {@code MappingJackson2HttpMessageConverter} 안에서 맨 {@code new ObjectMapper()} 를 쓴다.</b>
 *
 * <p>2026-08-07 실측 — 이 애노테이션이 없어 카카오가 200 과 {@code access_token} 을 정상 반환했는데도 <b>모든 필드가 {@code
 * null}</b> 이 됐고, {@code KakaoOAuthClient} 가 "no access token" 으로 판단해 502 를 던졌다. <b>인가코드는 이미 소비된 뒤라
 * 재시도도 불가능했다.</b> 카카오 응답 로그에는 값이 멀쩡히 찍혀 있어 외부 API 장애로 오진하기 쉽다.
 */
public record KakaoTokenResponse(
    @JsonProperty("access_token") String accessToken,
    @JsonProperty("token_type") String tokenType,
    @JsonProperty("refresh_token") String refreshToken,
    @JsonProperty("expires_in") Integer expiresIn,
    @JsonProperty("scope") String scope,
    @JsonProperty("refresh_token_expires_in") Integer refreshTokenExpiresIn) {}
