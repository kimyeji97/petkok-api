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
 * {@code V5__rename_at_to_date_columns.sql} 의 형태 계약. 검증 계약 REQ-17-01 · 02 (PLAN-REQ-17 § 검증 계약).
 *
 * <p><b>이 두 케이스는 Phase 2 전까지 실패한다.</b> 마이그레이션 파일이 아직 없기 때문이다 — {@code V3TimestamptzMigrationTest} 와
 * 같은 자리이고, 통과하는 가짜 테스트 대신 빨간불을 남긴다.
 *
 * <p>SQL 을 파싱하지 않고 텍스트로 센다 — 이 레포에는 SQL 파서가 없다.
 */
class V5RenameAtToDateColumnsMigrationTest {

  private static final String MIGRATION = "/db/migration/V5__rename_at_to_date_columns.sql";

  private static String flatSql() throws IOException {
    try (InputStream in =
        V5RenameAtToDateColumnsMigrationTest.class.getResourceAsStream(MIGRATION)) {
      assertThat(in).as("마이그레이션 파일이 클래스패스에 없다: %s (Phase 2 미착수)", MIGRATION).isNotNull();
      return new String(in.readAllBytes(), StandardCharsets.UTF_8)
          .replaceAll("\\s+", " ")
          .toLowerCase(Locale.ROOT);
    }
  }

  @Test
  @DisplayName(
      "[REQ-17-01] weight_logs.measured_at·photos.taken_at 두 컬럼을 각각 measured_date·taken_date 로 rename한다")
  void req_17_01_renamesBothDateColumns() throws IOException {
    String flat = flatSql();
    assertThat(flat)
        .as("weight_logs 컬럼 rename 누락 — measured_at 이 남으면 응답 계약(measured_date)과 어긋난다")
        .containsPattern(Pattern.compile("rename\\s+column\\s+measured_at\\s+to\\s+measured_date"));
    assertThat(flat)
        .as("photos 컬럼 rename 누락 — taken_at 이 남으면 응답 계약(taken_date)과 어긋난다")
        .containsPattern(Pattern.compile("rename\\s+column\\s+taken_at\\s+to\\s+taken_date"));
  }

  @Test
  @DisplayName(
      "[REQ-17-02] idx_weight_pet_measured_at 인덱스를 idx_weight_pet_measured_date 로 rename한다")
  void req_17_02_renamesWeightIndex() throws IOException {
    // Postgres 의 인덱스 리네임 문법은 `ALTER INDEX <이름> RENAME TO <새 이름>` 이다 — `RENAME INDEX` 는
    // 없는 문법이다(MySQL 과 혼동하기 쉽다). 기존 정규식이 이 순서를 잘못 가정해 REQ-17-02 를 실패시켰다
    // (테스트 결함, /testrun 수정 — PLAN-REQ-17 §검증 계약).
    assertThat(flatSql())
        .as("컬럼만 바꾸고 인덱스명을 그대로 두면 인덱스명과 컬럼명이 어긋난 채 남는다 (PLAN-REQ-17 §제약·함정)")
        .containsPattern(
            Pattern.compile(
                "alter\\s+index\\s+idx_weight_pet_measured_at\\s+rename\\s+to\\s+idx_weight_pet_measured_date"));
  }
}
