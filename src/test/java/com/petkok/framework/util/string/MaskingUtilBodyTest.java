package com.petkok.framework.util.string;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 요청·응답 본문 자격증명 마스킹 계약. 검증 계약 REQ-07-09 ~ 11 (PLAN-REQ-07 § 검증 계약).
 *
 * <p>Phase 4 완료 기준이 <b>"로그에 토큰 원문이 남지 않는다"</b>이다. 요청 쪽(form 본문)은 잘못된 인가코드로 실제 카카오 왕복을 태워 눈으로 확인했지만,
 * <b>성공 응답은 유효한 1회용 인가코드가 있어야 재현되므로</b> 실물로 태울 수 없다. 그 경로를 여기서 고정한다.
 */
class MaskingUtilBodyTest {

  /** 카카오 토큰 교환 성공 응답의 실제 형태. 이 본문이 그대로 로그에 찍히면 AGENTS.md §5 위반이다. */
  private static final String KAKAO_TOKEN_RESPONSE =
      "{\"token_type\":\"bearer\","
          + "\"access_token\":\"E0aBcDeFgHiJkLmNoPqRsTuVwXyZ0123456789abcdef\","
          + "\"expires_in\":21599,"
          + "\"refresh_token\":\"R9zYxWvUtSrQpOnMlKjIhGfEdCbA9876543210zyxwv\","
          + "\"refresh_token_expires_in\":5183999,"
          + "\"scope\":\"profile_image profile_nickname\"}";

  @Test
  @DisplayName("[REQ-07-09] 토큰 응답 본문의 access_token·refresh_token 원문이 남지 않는다")
  void req_07_09_jsonTokensAreMasked() {
    String masked = MaskingUtil.maskingCredentialsInBody(KAKAO_TOKEN_RESPONSE);

    assertThat(masked)
        .doesNotContain("E0aBcDeFgHiJkLmNoPqRsTuVwXyZ0123456789abcdef")
        .doesNotContain("R9zYxWvUtSrQpOnMlKjIhGfEdCbA9876543210zyxwv")
        .contains("\"access_token\":\"E0aB***\"")
        .contains("\"refresh_token\":\"R9zY***\"");
    // 진단에 필요한 값은 그대로 남아야 한다 — 값의 형태가 아니라 키 이름으로 판단하기 때문이다.
    assertThat(masked).contains("\"expires_in\":21599").contains("profile_nickname");
  }

  @Test
  @DisplayName("[REQ-07-10] 토큰 교환 요청 form 본문의 client_secret·client_id·code 가 마스킹된다")
  void req_07_10_formCredentialsAreMasked() {
    // ⚠️ 전부 더미다. 실측 로그에서 값을 복사해 오지 말 것 — 2026-07-29 에 실제 비밀번호가
    //    public 레포에 커밋된 적이 있다 (CLAUDE.md).
    String form =
        "grant_type=authorization_code"
            + "&client_id=DUMMYRESTAPIKEY0123456789abcdef"
            + "&redirect_uri=http%3A%2F%2Flocalhost%3A3000%2Foauth%2Fkakao%2Fcallback"
            + "&code=AUTHORIZATION_CODE_VALUE"
            + "&client_secret=SUPERSECRETVALUE";

    String masked = MaskingUtil.maskingCredentialsInBody(form);

    assertThat(masked)
        .doesNotContain("DUMMYRESTAPIKEY0123456789abcdef")
        .doesNotContain("AUTHORIZATION_CODE_VALUE")
        .doesNotContain("SUPERSECRETVALUE")
        .contains("client_id=DUMM***")
        .contains("code=AUTH***")
        .contains("client_secret=SUPE***");
    // grant_type·redirect_uri 는 민감값이 아니라 그대로 남는다. redirect_uri 불일치는 KOE006 의 원인이라 진단에 필요하다.
    assertThat(masked)
        .contains("grant_type=authorization_code")
        .contains("redirect_uri=http%3A%2F%2Flocalhost%3A3000%2Foauth%2Fkakao%2Fcallback");
  }

  @Test
  @DisplayName("[REQ-07-11] 카카오 오류 응답의 진단 정보는 가리지 않는다")
  void req_07_11_diagnosticFieldsSurvive() {
    // /v2/user/me 의 허용 IP 거부. code 가 숫자라 JSON 규칙(문자열 값)에 걸리지 않아야 한다 —
    // 이걸 가리면 "키 문제인지 IP 문제인지" 구분이 막힌다.
    String ipMismatch = "{\"code\":-401,\"msg\":\"ip mismatched!\"}";

    assertThat(MaskingUtil.maskingCredentialsInBody(ipMismatch)).isEqualTo(ipMismatch);
  }
}
