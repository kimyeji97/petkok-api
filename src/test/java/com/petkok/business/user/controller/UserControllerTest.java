package com.petkok.business.user.controller;

import static org.assertj.core.api.Assertions.assertThat;

import com.petkok.framework.security.AuthPrincipal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * 탈퇴 엔드포인트의 응답 계약. 검증 계약 REQ-08-15 (PLAN-REQ-08 § 검증 계약).
 *
 * <p><b>애노테이션과 시그니처를 고정한다.</b> Notion {@code API I/F} 는 "Response 204: 응답 바디 없음"만 규정하는데, 이 계약은
 * {@code @ResponseStatus(NO_CONTENT)} + {@code void} 반환 <b>두 가지가 모두 있어야</b> 성립한다. 하나라도 빠지면 200 이
 * 나가거나 본문이 붙는다. {@code AuthController.logout} 과 같은 형태다.
 *
 * <p>⚠️ <b>이것은 HTTP 왕복 검증이 아니다.</b> 실제 응답 코드·본문은 확인하지 않는다 — 그러려면 {@code @WebMvcTest} 가 필요한데 이 레포에
 * 아직 없는 패턴이다. 같은 이유로 {@code GET /users/me} 의 <b>전역 snake_case 직렬화</b>와 101자 닉네임이 <b>400 으로
 * 나가는지</b>도 여전히 검증되지 않는다(PLAN-REQ-08 Phase 1 참고). 셋은 {@code @WebMvcTest} 도입 시 함께 닫힌다.
 */
class UserControllerTest {

  @Test
  @DisplayName("[REQ-08-15] DELETE /me 는 204 를 반환한다")
  void req_08_15_withdrawReturnsNoContent() throws ReflectiveOperationException {
    ResponseStatus status =
        UserController.class
            .getMethod("withdraw", AuthPrincipal.class)
            .getAnnotation(ResponseStatus.class);

    assertThat(status)
        .isNotNull()
        .extracting(ResponseStatus::value)
        .isEqualTo(HttpStatus.NO_CONTENT);
  }

  @Test
  @DisplayName("[REQ-08-15] DELETE /me 는 응답 본문이 없다 (ApiResponse 로 감싸지 않는다)")
  void req_08_15_withdrawHasNoResponseBody() throws ReflectiveOperationException {
    Class<?> returnType =
        UserController.class.getMethod("withdraw", AuthPrincipal.class).getReturnType();

    assertThat(returnType).isEqualTo(void.class);
  }
}
