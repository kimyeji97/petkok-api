package com.petkok.framework.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Field;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.util.AntPathMatcher;

/**
 * 공개 경로 범위 계약. 검증 계약 REQ-07-01 ~ 03 (PLAN-REQ-07 § 검증 계약, AGENTS §5).
 *
 * <p><b>이 세 케이스는 Phase 5 까지 실패한다.</b> 현재 {@code PUBLIC_PATHS} 가 {@code "/api/v1/auth/**"} 이고, 계획서가
 * 「제약·함정」에 <em>"Phase 5에서 반드시 개별 경로로 좁힐 것"</em> 이라 적어 둔 항목이다. 아직 auth 컨트롤러가 없어 실제로 노출된 엔드포인트는 없지만,
 * {@code DELETE /auth/logout} 이 생기는 순간 무인증으로 열린다.
 *
 * <p><b>{@code logout} 판정에 문자열 포함이 아니라 {@link AntPathMatcher} 를 쓰는 이유가 여기 있다.</b> 와일드카드 {@code
 * /api/v1/auth/**} 는 "logout" 이라는 글자를 담고 있지 않지만 {@code /api/v1/auth/logout} 을 <b>매칭한다.</b> 문자열 검사로는
 * 정확히 이 사고를 놓친다.
 *
 * <p>{@code PUBLIC_PATHS} 가 {@code private static} 이라 리플렉션으로 읽는다. Spring 컨텍스트를 띄우지 않고 값 자체를 계약으로
 * 고정하기 위해서다.
 */
class SecurityConfigPublicPathsTest {

  private static final String LOGOUT_PATH = "/api/v1/auth/logout";

  private static String[] publicPaths() throws ReflectiveOperationException {
    Field field = SecurityConfig.class.getDeclaredField("PUBLIC_PATHS");
    field.setAccessible(true);
    return (String[]) field.get(null);
  }

  @Test
  @DisplayName("[REQ-07-01] 공개 경로는 kakao·refresh·health 3개뿐이다")
  void req_07_01_publicPathsAreExactlyThree() throws ReflectiveOperationException {
    assertThat(publicPaths())
        .containsExactlyInAnyOrder(
            "/api/v1/auth/kakao", "/api/v1/auth/refresh", "/actuator/health");
  }

  @Test
  @DisplayName("[REQ-07-02] logout 은 어떤 공개 경로에도 매칭되지 않는다")
  void req_07_02_logoutIsNotPublic() throws ReflectiveOperationException {
    AntPathMatcher matcher = new AntPathMatcher();

    assertThat(publicPaths()).noneMatch(pattern -> matcher.match(pattern, LOGOUT_PATH));
  }

  @Test
  @DisplayName("[REQ-07-03] 공개 경로에 와일드카드를 쓰지 않는다")
  void req_07_03_noWildcardInPublicPaths() throws ReflectiveOperationException {
    assertThat(publicPaths()).noneMatch(pattern -> pattern.contains("*"));
  }
}
