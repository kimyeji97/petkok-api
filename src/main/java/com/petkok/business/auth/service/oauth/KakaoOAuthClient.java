package com.petkok.business.auth.service.oauth;

import com.petkok.data.auth.dto.KakaoTokenResponse;
import com.petkok.data.auth.dto.KakaoUserResponse;
import com.petkok.data.auth.dto.OAuthProfileResponse;
import com.petkok.framework.config.KakaoProperties;
import com.petkok.framework.exception.BusinessException;
import com.petkok.framework.exception.ErrorCode;
import com.petkok.framework.util.http.RestClientBase;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

/**
 * 카카오 OAuth2 클라이언트. <b>커스텀 플로우</b>다 — 클라이언트가 받은 인가코드를 서버가 넘겨받아 토큰으로 교환하고 프로필을 조회할 뿐,
 * spring-security-oauth2-client 의 리다이렉트 로그인이 아니다.
 *
 * <p>2026-07-29 에 curl 로 이 왕복(인가코드 → 토큰 교환 → {@code /v2/user/me})을 먼저 통과시킨 뒤 코드로 옮겼다. 콘솔 설정 문제와 서버
 * 코드 문제가 섞이면 원인 분리가 어렵기 때문이다.
 */
@Slf4j
@Component
public class KakaoOAuthClient extends RestClientBase {

  private static final String TOKEN_URL = "https://kauth.kakao.com/oauth/token";
  private static final String USER_INFO_URL = "https://kapi.kakao.com/v2/user/me";
  private static final String HTTP_SCHEME = "http://";
  private static final String HTTPS_SCHEME = "https://";

  private final KakaoProperties properties;

  public KakaoOAuthClient(KakaoProperties properties) {
    this.properties = properties;
  }

  /**
   * 인가코드를 카카오 토큰으로 교환한다.
   *
   * <p>⚠️ <b>인가 코드는 1회용이고 약 10분 만료다.</b> 이 호출이 성공한 뒤 뒷단에서 실패하면 코드는 이미 소비돼 같은 코드로 재시도할 수 없다.
   *
   * @param authorizationCode 클라이언트가 카카오에서 받아 서버로 넘긴 인가코드
   */
  public KakaoTokenResponse exchangeToken(String authorizationCode) {
    MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
    form.add("grant_type", "authorization_code");
    form.add("client_id", properties.clientId());
    form.add("redirect_uri", properties.redirectUri());
    form.add("code", authorizationCode);
    // 빈 값을 실어 보내면 카카오가 거부한다. 콘솔에서 "사용함"인 경우에만 파라미터를 넣는다.
    if (properties.hasClientSecret()) {
      form.add("client_secret", properties.clientSecret());
    }

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

    ResponseEntity<KakaoTokenResponse> response =
        post(TOKEN_URL, headers, form, KakaoTokenResponse.class);
    KakaoTokenResponse body = response == null ? null : response.getBody();
    if (body == null || StringUtils.isBlank(body.accessToken())) {
      log.error("Kakao token exchange returned no access token.");
      throw new BusinessException(ErrorCode.EXTERNAL_API_ERROR, "카카오 토큰 교환에 실패했습니다.");
    }
    return body;
  }

  /**
   * 카카오 access token 으로 프로필을 조회해 정규화된 형태로 돌려준다.
   *
   * <p>⚠️ 이 호출만 콘솔 「허용 IP 주소」의 영향을 받는다. 토큰 교환은 되는데 여기서 {@code -401 ip mismatched!} 가 나면 키가 아니라 IP
   * 문제다.
   *
   * @param kakaoAccessToken {@link #exchangeToken} 이 받아온 <b>카카오</b> access token
   */
  public OAuthProfileResponse getProfile(String kakaoAccessToken) {
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(kakaoAccessToken);
    headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

    ResponseEntity<KakaoUserResponse> response =
        get(USER_INFO_URL, headers, KakaoUserResponse.class);
    KakaoUserResponse body = response == null ? null : response.getBody();
    if (body == null || body.id() == null) {
      log.error("Kakao user info returned no id.");
      throw new BusinessException(ErrorCode.EXTERNAL_API_ERROR, "카카오 사용자 정보를 가져오지 못했습니다.");
    }

    KakaoUserResponse.ProfileResponse profile =
        body.kakaoAccount() == null ? null : body.kakaoAccount().profile();
    String nickname = profile == null ? null : profile.nickname();
    if (StringUtils.isBlank(nickname)) {
      // users.nickname 이 NOT NULL 이다. profile_nickname 동의항목이 꺼져 있으면 여기서 걸린다.
      log.error("Kakao profile has no nickname. Check the profile_nickname consent item.");
      throw new BusinessException(ErrorCode.EXTERNAL_API_ERROR, "카카오 프로필 닉네임을 가져오지 못했습니다.");
    }

    return new OAuthProfileResponse(
        String.valueOf(body.id()),
        nickname,
        body.kakaoAccount() == null ? null : body.kakaoAccount().email(),
        toHttps(profile.profileImageUrl()));
  }

  /**
   * 프로필 이미지 URL 을 {@code https} 로 정규화한다.
   *
   * <p>카카오는 {@code http://k.kakaocdn.net/...} 로 내려주는데 그대로 클라이언트에 전달하면 <b>iOS ATS·Android cleartext
   * 정책에 막힌다.</b> 같은 경로를 {@code https} 로 요청하면 200 이 나오는 것을 2026-07-29 에 확인했으므로 저장 시점에 바꾼다.
   */
  private static String toHttps(String url) {
    if (url == null || !url.startsWith(HTTP_SCHEME)) {
      return url;
    }
    return HTTPS_SCHEME + url.substring(HTTP_SCHEME.length());
  }
}
