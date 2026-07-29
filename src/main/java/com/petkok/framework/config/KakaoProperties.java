package com.petkok.framework.config;

import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Kakao OAuth2 설정. <b>커스텀 플로우라 값이 3개뿐이다</b> — 클라이언트가 받은 인가코드를 서버가 넘겨받아 토큰으로 교환할 뿐, {@code
 * spring-security-oauth2-client} 의 리다이렉트 로그인이 아니다. 그쪽 설정 트리({@code registration}/{@code provider})는
 * 통째로 필요 없다.
 *
 * @param clientId 콘솔의 앱 키 4종 중 <b>REST API 키</b>. 네이티브·JavaScript 키가 아니며 Admin 키는 전권이라 서버에도 두지 않는다
 * @param clientSecret 콘솔에서 "사용함"으로 켠 경우에만 필요. 비어 있는 상태가 정상 시나리오다 — {@link #hasClientSecret()} 참고
 * @param redirectUri 서버가 리다이렉트를 받지는 않지만 토큰 교환 요청에 들어간다. <b>콘솔 등록값·클라이언트가 인가 요청에 쓴 값과 문자 단위로 같아야</b>
 *     하며 다르면 {@code KOE006}/{@code invalid_grant} 로 떨어진다
 */
@ConfigurationProperties(prefix = "kakao")
public record KakaoProperties(String clientId, String clientSecret, String redirectUri) {

  /**
   * client secret 을 토큰 교환 요청에 실을지 여부.
   *
   * <p><b>빈 값을 실어 보내면 카카오가 거부한다.</b> 미사용 앱에서는 파라미터 자체를 생략해야 한다. {@code application.yml} 의 기본값이 빈
   * 문자열인 것도 이 때문이다.
   */
  public boolean hasClientSecret() {
    return StringUtils.isNotBlank(clientSecret);
  }
}
