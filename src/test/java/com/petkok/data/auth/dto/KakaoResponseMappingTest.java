package com.petkok.data.auth.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 카카오 응답의 snake_case 필드 매핑. 검증 계약 REQ-07-24 · 25 (PLAN-REQ-07 § 검증 계약).
 *
 * <p><b>맨 {@code new ObjectMapper()} 를 쓰는 것이 이 테스트의 핵심이다.</b> 전역 snake_case 설정을 <b>일부러 적용하지 않는다</b>
 * — 이 DTO 를 실제로 읽는 것은 {@code RestTemplateConfig} 의 {@code new RestTemplate()} 이고, 그건 자기 {@code
 * MappingJackson2HttpMessageConverter} 안에서 설정 없는 ObjectMapper 를 쓰기 때문이다. 설정된 매퍼로 검증하면 <b>버그가 있어도
 * 초록불이 된다.</b>
 *
 * <p>2026-08-07 실측 — {@code @JsonProperty} 가 없어 카카오가 200 과 {@code access_token} 을 정상 반환했는데도 모든 필드가
 * {@code null} 이 됐다. 카카오 응답 로그에는 값이 멀쩡히 찍혀 있었고 우리 쪽은 502 를 던져, <b>외부 API 장애로 오진하기 딱 좋은 형태</b>였다.
 * 인가코드는 이미 소비된 뒤라 재시도도 불가능했다.
 *
 * <p>본문은 2026-08-07 왕복에서 받은 <b>실제 응답 형태</b>다(값은 더미로 교체).
 */
class KakaoResponseMappingTest {

  private final ObjectMapper plainMapper = new ObjectMapper();

  @Test
  @DisplayName("[REQ-07-24] 토큰 교환 응답의 access_token 이 매핑된다 (설정 없는 ObjectMapper 기준)")
  void req_07_24_tokenResponseMapsSnakeCaseFields() throws Exception {
    String body =
        """
        {"access_token":"dummy-access","token_type":"bearer","refresh_token":"dummy-refresh",\
        "expires_in":21599,"scope":"profile_image profile_nickname",\
        "refresh_token_expires_in":5183999}""";

    KakaoTokenResponse actual = plainMapper.readValue(body, KakaoTokenResponse.class);

    assertThat(actual.accessToken()).isEqualTo("dummy-access");
  }

  @Test
  @DisplayName("[REQ-07-25] 프로필 응답의 kakao_account.profile.nickname 이 매핑된다")
  void req_07_25_userResponseMapsNestedSnakeCaseFields() throws Exception {
    String body =
        """
        {"id":1234567890,"kakao_account":{"email":null,"profile":{"nickname":"게코집사",\
        "profile_image_url":"http://k.kakaocdn.net/dummy.jpg",\
        "thumbnail_image_url":"http://k.kakaocdn.net/dummy_110x110.jpg"}}}""";

    KakaoUserResponse actual = plainMapper.readValue(body, KakaoUserResponse.class);

    assertThat(actual.kakaoAccount().profile().nickname()).isEqualTo("게코집사");
  }

  @Test
  @DisplayName("[REQ-07-25] 프로필 응답의 profile_image_url 이 매핑된다")
  void req_07_25_userResponseMapsProfileImageUrl() throws Exception {
    String body =
        """
        {"id":1234567890,"kakao_account":{"profile":{"nickname":"게코집사",\
        "profile_image_url":"http://k.kakaocdn.net/dummy.jpg"}}}""";

    KakaoUserResponse actual = plainMapper.readValue(body, KakaoUserResponse.class);

    assertThat(actual.kakaoAccount().profile().profileImageUrl())
        .isEqualTo("http://k.kakaocdn.net/dummy.jpg");
  }
}
