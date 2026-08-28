package com.petkok.data.activity.dto;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.Arrays;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** 활동 DTO 의 형태 계약. 검증 계약 REQ-10-38 (PLAN-REQ-10 § 검증 계약). */
class ActivityDtoContractTest {

  @Test
  @DisplayName("[REQ-10-38] 수정 요청 DTO 에 @NotBlank·@NotNull 이 없다")
  void req_10_38_updateRequestHasNoNotBlankOrNotNull() {
    boolean hasRejectingNull =
        Arrays.stream(ActivityUpdateRequest.class.getRecordComponents())
            .anyMatch(
                c -> c.isAnnotationPresent(NotBlank.class) || c.isAnnotationPresent(NotNull.class));

    assertThat(hasRejectingNull).isFalse();
  }
}
