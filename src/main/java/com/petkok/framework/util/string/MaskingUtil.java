package com.petkok.framework.util.string;

import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;

/** 마스킹처리 유틸리티 */
public class MaskingUtil {

  private MaskingUtil() {}

  /**
   * 이름 가운데 글자 마스킹
   *
   * @param name 이름
   * @return 마스킹된 이름
   */
  public static String maskingMiddleName(String name) {
    if (name == null) {
      return null;
    }
    // 한글만 (영어, 숫자 포함 이름은 제외)
    String regex = "(^[가-힣]+)$";

    Matcher matcher = Pattern.compile(regex).matcher(name);
    if (matcher.find()) {
      int length = name.length();

      String middleMask;
      if (length > 2) {
        middleMask = name.substring(1, length - 1);
      } else {
        // 이름이 외자
        middleMask = name.substring(1, length);
      }

      StringBuilder dot = new StringBuilder();
      for (int i = 0; i < middleMask.length(); i++) {
        dot.append("*");
      }

      if (length > 2) {
        return name.charAt(0)
            + middleMask.replace(middleMask, dot.toString())
            + name.substring(length - 1, length);
      } else {
        // 이름이 외자 마스킹 리턴
        return name.charAt(0) + middleMask.replace(middleMask, dot.toString());
      }
    }
    return name;
  }

  /**
   * Second 이름 마스킨
   *
   * @param name 이름
   * @return 마스킹된 이름
   */
  public static String maskingSecondName(String name) {
    if (StringUtils.isEmpty(name)) {
      return "";
    }

    StringBuilder sb = new StringBuilder();
    sb.append(name.charAt(0));
    sb.append("*".repeat(name.length() - 1));
    return sb.toString();
  }

  /**
   * 자격증명(토큰·API 키 등) 마스킹. 식별 가능한 앞 4자리만 남기고 나머지를 가린다. 로그에 토큰 원문이 남지 않도록 하되, 어떤 값이 쓰였는지는 구분할 수 있게
   * 한다.
   *
   * @param credential 토큰·키 등 민감 문자열
   * @return 마스킹된 문자열 (null 입력 시 null, 5자 미만이면 전체 마스킹)
   */
  public static String maskingCredential(String credential) {
    if (credential == null) {
      return null;
    }
    if (credential.length() < 5) {
      return "*".repeat(credential.length());
    }
    return credential.substring(0, 4) + "***";
  }

  /**
   * JSON 본문에서 값을 가려야 하는 키. 문자열 값만 대상으로 한다.
   *
   * <p>{@code code} 는 여기 없다 — 인가 코드는 form 요청에만 실리고, 오히려 카카오 오류 응답의 {@code {"code":-401,"msg":"ip
   * mismatched!"}} 처럼 진단에 필요한 값이 같은 이름을 쓴다. 그걸 가리면 원인 파악이 막힌다.
   */
  private static final Pattern JSON_CREDENTIAL =
      Pattern.compile(
          "(\"(?:access_token|refresh_token|id_token|client_secret|token|secret)\"\\s*:\\s*\")"
              + "([^\"]*)(\")");

  /**
   * form-urlencoded 본문에서 값을 가려야 하는 파라미터.
   *
   * <p>{@code code}(인가 코드)는 1회용이지만 로그에 남길 이유가 없어 포함한다.
   *
   * <p>{@code client_id}(카카오 REST API 키)도 포함한다 — {@code client_secret} 만큼은 아니어도 앱을 식별하는 서버측 키이고, 실측
   * 로그에서 전체 값이 평문으로 찍히는 것을 2026-07-29 에 확인했다. 앞 4자는 남으므로 어떤 앱 키가 쓰였는지는 여전히 구분된다.
   */
  private static final Pattern FORM_CREDENTIAL =
      Pattern.compile(
          "((?:^|&)(?:access_token|refresh_token|id_token|client_secret|client_id|token|secret|code)=)"
              + "([^&]*)");

  /**
   * 요청·응답 <b>본문</b>의 자격증명 값을 마스킹한다. 헤더 마스킹({@link #maskingCredential})과 짝이다.
   *
   * <p>필요한 이유: 카카오 토큰 교환은 <b>응답 본문</b>에 {@code access_token}·{@code refresh_token} 을 평문으로 담아 준다.
   * 헤더만 가리면 토큰이 그대로 로그에 남는다 (AGENTS.md §5 위반). 요청 쪽도 form 본문에 {@code client_secret} 과 인가 코드가 실린다.
   *
   * <p>JSON 과 form-urlencoded 를 모두 처리한다. 값의 형태만 보고 자르지 않고 <b>키 이름으로</b> 판단하므로, 카카오 오류 응답의 {@code
   * msg} 처럼 진단에 필요한 값은 그대로 남는다.
   *
   * @param body 원본 본문. {@code null}·공백이면 그대로 반환한다
   * @return 자격증명 값이 {@code 앞4자+***} 로 바뀐 본문
   */
  public static String maskingCredentialsInBody(String body) {
    if (StringUtils.isBlank(body)) {
      return body;
    }
    String masked = replaceCredential(JSON_CREDENTIAL, body);
    return replaceCredential(FORM_CREDENTIAL, masked);
  }

  /** 캡처 그룹 2(값)만 마스킹하고 나머지는 그대로 둔다. */
  private static String replaceCredential(Pattern pattern, String body) {
    Matcher matcher = pattern.matcher(body);
    StringBuilder sb = new StringBuilder();
    while (matcher.find()) {
      String replacement = matcher.group(1) + maskingCredential(matcher.group(2));
      if (matcher.groupCount() >= 3) {
        replacement += matcher.group(3);
      }
      matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
    }
    matcher.appendTail(sb);
    return sb.toString();
  }

  /**
   * 핸드폰번호 가운데 마스킹
   *
   * @param phoneNo 전화번호
   * @return 마스킹된 전화번호
   */
  public static String maskingMiddlePhoneNo(String phoneNo) {
    String regex = "(\\d{2,3})-?(\\d{3,4})-?(\\d{4})$";

    Matcher matcher = Pattern.compile(regex).matcher(phoneNo);
    if (matcher.find()) {
      String target = matcher.group(2);
      int length = target.length();
      char[] c = new char[length];
      Arrays.fill(c, '*');

      return phoneNo.replace(target, String.valueOf(c));
    }
    return phoneNo;
  }
}
