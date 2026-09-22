package com.petkok.business.gallery.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 사진 자유 태그 정규화 — <b>I/O 없는 순수 클래스</b>(「소스 구조」 §1-4·§8, {@code EnvironmentSummaryCalculator}와 같은
 * 패턴). 검증 계약 REQ-22-04 ~ 07 (PLAN-REQ-22 § 검증 계약).
 *
 * <p>⚠️ 이 파일은 {@code PhotoTagNormalizer}가 아직 없어 컴파일되지 않는다 — {@code /implement REQ-22 1}이 만든다.
 */
class PhotoTagNormalizerTest {

  @Test
  @DisplayName("[REQ-22-04] 태그 앞뒤 공백을 제거한다")
  void req_22_04_trimsLeadingAndTrailingWhitespace() {
    List<String> result = PhotoTagNormalizer.normalize(List.of("  탈피  "));

    assertThat(result).containsExactly("탈피");
  }

  @Test
  @DisplayName("[REQ-22-05] 태그 내부 공백을 제거한다")
  void req_22_05_removesInternalWhitespace() {
    List<String> result = PhotoTagNormalizer.normalize(List.of("핸들링 완료"));

    assertThat(result).containsExactly("핸들링완료");
  }

  @Test
  @DisplayName("[REQ-22-06] 공백 제거 후 빈 문자열이 되면 그 태그는 무시한다")
  void req_22_06_ignoresTagThatBecomesEmptyAfterTrimming() {
    List<String> result = PhotoTagNormalizer.normalize(List.of("탈피", "   ", ""));

    assertThat(result).containsExactly("탈피");
  }

  @Test
  @DisplayName("[REQ-22-07] 중복 태그는 한 번만 남는다")
  void req_22_07_duplicateTagsCollapseToOne() {
    List<String> result = PhotoTagNormalizer.normalize(List.of("탈피", "탈피", " 탈피 "));

    assertThat(result).containsExactly("탈피");
  }
}
