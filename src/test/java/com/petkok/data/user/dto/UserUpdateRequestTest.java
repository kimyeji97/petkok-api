package com.petkok.data.user.dto;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * {@code PATCH /users/me} 요청의 검증 규칙. 검증 계약 REQ-08-06 · 07 (PLAN-REQ-08 § 검증 계약).
 *
 * <p>Spring 컨텍스트를 띄우지 않고 Bean Validation 을 직접 돌린다 — 검증 대상이 애노테이션 그 자체라 컨트롤러 왕복이 필요 없다.
 */
class UserUpdateRequestTest {

  private static final ValidatorFactory FACTORY = Validation.buildDefaultValidatorFactory();
  private final Validator validator = FACTORY.getValidator();

  private static String repeat(int length) {
    return "가".repeat(length);
  }

  @Test
  @DisplayName("[REQ-08-06] 101자 닉네임은 검증에서 걸린다 (DB 제약 위반으로 500 이 되지 않는다)")
  void req_08_06_nicknameOver100IsRejected() {
    var violations = validator.validate(new UserUpdateRequest(repeat(101), null));

    assertThat(violations).hasSize(1);
  }

  @Test
  @DisplayName("[REQ-08-07] 100자 닉네임은 통과한다")
  void req_08_07_nicknameAtMaxIsAccepted() {
    var violations = validator.validate(new UserUpdateRequest(repeat(100), null));

    assertThat(violations).isEmpty();
  }

  @Test
  @DisplayName("[REQ-08-07] null 닉네임은 통과한다 (누락 = 변경 없음)")
  void req_08_07_nullNicknameIsAccepted() {
    var violations = validator.validate(new UserUpdateRequest(null, null));

    assertThat(violations).isEmpty();
  }
}
