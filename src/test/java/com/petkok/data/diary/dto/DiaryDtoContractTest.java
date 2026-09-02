package com.petkok.data.diary.dto;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.Arrays;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 다이어리 DTO 의 형태 계약. 검증 계약 REQ-10-100 (PLAN-REQ-10 § 검증 계약).
 *
 * <p>⚠️ 이 파일은 {@code DiaryUpdateRequest} 가 아직 없어 컴파일되지 않는다 — {@code /implement REQ-10 5} 가 만든다.
 */
class DiaryDtoContractTest {

  @Test
  @DisplayName("[REQ-10-100] 수정 요청 DTO 에 @NotBlank·@NotNull 이 없다")
  void req_10_100_updateRequestHasNoNotBlankOrNotNull() {
    boolean hasRejectingNull =
        Arrays.stream(DiaryUpdateRequest.class.getRecordComponents())
            .anyMatch(
                c -> c.isAnnotationPresent(NotBlank.class) || c.isAnnotationPresent(NotNull.class));

    assertThat(hasRejectingNull).isFalse();
  }
}
