package com.petkok.business.gallery.service;

import java.util.LinkedHashSet;
import java.util.List;

/**
 * 사진 자유 태그 정규화 — <b>I/O 없는 순수 클래스</b>(「소스 구조」 §1-4·§8, {@code EnvironmentSummaryCalculator}와 같은
 * 패턴). 검증 계약 REQ-22-04 ~ 07 (PLAN-REQ-22 § 검증 계약).
 *
 * <p>고정 목록이 아니라 사용자가 그때그때 입력하는 자유 태그다(인스타 해시태그 유비) — 앞뒤·내부 공백을 전부 제거하고, 제거 후 빈 문자열이 되면 그 태그는 버린다.
 * 중복은 한 번만 남긴다({@code photo_tags} 의 {@code (photo_id, tag)} 유니크 제약과 맞물린다).
 */
public final class PhotoTagNormalizer {

  private PhotoTagNormalizer() {}

  public static List<String> normalize(List<String> rawTags) {
    LinkedHashSet<String> normalized = new LinkedHashSet<>();
    for (String rawTag : rawTags) {
      String stripped = rawTag.replaceAll("\\s+", "");
      if (!stripped.isEmpty()) {
        normalized.add(stripped);
      }
    }
    return List.copyOf(normalized);
  }
}
