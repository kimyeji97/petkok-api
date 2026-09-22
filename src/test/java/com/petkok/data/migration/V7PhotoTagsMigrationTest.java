package com.petkok.data.migration;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.regex.Pattern;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * {@code V7__photo_tags.sql} 의 형태 계약. 검증 계약 REQ-22-01 ~ 03 (PLAN-REQ-22 § 검증 계약).
 *
 * <p>⚠️ 이 파일은 마이그레이션이 아직 없어 컴파일 전까지도 실패한다(리소스 자체가 없다) — {@code /implement REQ-22 1} 이 만든다. {@code
 * V6EnvironmentLogsMigrationTest} 와 같은 자리다.
 *
 * <p>SQL 을 파싱하지 않고 텍스트로 센다 — 이 레포에는 SQL 파서가 없다.
 */
class V7PhotoTagsMigrationTest {

  private static final String MIGRATION = "/db/migration/V7__photo_tags.sql";

  private static String flatSql() throws IOException {
    try (InputStream in = V7PhotoTagsMigrationTest.class.getResourceAsStream(MIGRATION)) {
      assertThat(in).as("마이그레이션 파일이 클래스패스에 없다: %s (Phase 1 미착수)", MIGRATION).isNotNull();
      return new String(in.readAllBytes(), StandardCharsets.UTF_8)
          .replaceAll("\\s+", " ")
          .toLowerCase(Locale.ROOT);
    }
  }

  @Test
  @DisplayName("[REQ-22-01] photo_tags 테이블을 생성한다")
  void req_22_01_createsPhotoTagsTable() throws IOException {
    assertThat(flatSql())
        .as("photo_tags 테이블 생성 누락 — PLAN §결정 표 '태그 저장 구조'")
        .containsPattern(Pattern.compile("create\\s+table\\s+photo_tags"));
  }

  @Test
  @DisplayName("[REQ-22-02] photo_tags.tag 는 varchar(50)이다")
  void req_22_02_tagColumnIsVarchar50() throws IOException {
    assertThat(flatSql())
        .as("tag 컬럼 길이 제한 누락 — PLAN §결정 표 '태그 길이 제한' — \"varchar(50)\"")
        .containsPattern(Pattern.compile("tag\\s+varchar\\(50\\)"));
  }

  @Test
  @DisplayName("[REQ-22-03] photos 테이블에 is_representative 컬럼을 추가한다")
  void req_22_03_addsIsRepresentativeColumnToPhotos() throws IOException {
    assertThat(flatSql())
        .as("is_representative 컬럼 누락 — Phase 1 완료 기준 — \"photos.is_representative 컬럼 추가\"")
        .containsPattern(Pattern.compile("is_representative\\s+boolean"));
  }
}
