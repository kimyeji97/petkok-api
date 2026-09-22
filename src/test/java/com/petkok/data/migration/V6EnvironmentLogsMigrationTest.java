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
 * {@code V6__environment_logs.sql} 의 형태 계약. 검증 계약 REQ-20-01 (PLAN-REQ-20 § 검증 계약).
 *
 * <p>⚠️ 이 파일은 마이그레이션이 아직 없어 컴파일 전까지도 실패한다(리소스 자체가 없다) — {@code /implement REQ-20 1} 이 만든다. {@code
 * V5RenameAtToDateColumnsMigrationTest} 와 같은 자리다.
 *
 * <p>SQL 을 파싱하지 않고 텍스트로 센다 — 이 레포에는 SQL 파서가 없다.
 */
class V6EnvironmentLogsMigrationTest {

  private static final String MIGRATION = "/db/migration/V6__environment_logs.sql";

  private static String flatSql() throws IOException {
    try (InputStream in = V6EnvironmentLogsMigrationTest.class.getResourceAsStream(MIGRATION)) {
      assertThat(in).as("마이그레이션 파일이 클래스패스에 없다: %s (Phase 1 미착수)", MIGRATION).isNotNull();
      return new String(in.readAllBytes(), StandardCharsets.UTF_8)
          .replaceAll("\\s+", " ")
          .toLowerCase(Locale.ROOT);
    }
  }

  @Test
  @DisplayName("[REQ-20-01] environment_logs 테이블을 생성한다")
  void req_20_01_createsEnvironmentLogsTable() throws IOException {
    assertThat(flatSql())
        .as("environment_logs 테이블 생성 누락")
        .containsPattern(Pattern.compile("create\\s+table\\s+environment_logs"));
  }

  @Test
  @DisplayName("[REQ-20-01] environment_logs 는 온도 컬럼을 갖는다")
  void req_20_01_hasTemperatureColumn() throws IOException {
    assertThat(flatSql())
        .as("temperature 컬럼 누락 — PLAN §결정 표(decimal(4,1))")
        .containsPattern(Pattern.compile("temperature\\s+decimal\\(4,\\s*1\\)"));
  }

  @Test
  @DisplayName("[REQ-20-01] environment_logs 는 습도 컬럼을 갖는다")
  void req_20_01_hasHumidityColumn() throws IOException {
    assertThat(flatSql())
        .as("humidity 컬럼 누락 — PLAN §결정 표(decimal(4,1))")
        .containsPattern(Pattern.compile("humidity\\s+decimal\\(4,\\s*1\\)"));
  }

  @Test
  @DisplayName("[REQ-20-01] environment_logs 는 measured_at 컬럼을 timestamptz 로 갖는다")
  void req_20_01_hasMeasuredAtColumnAsTimestamptz() throws IOException {
    assertThat(flatSql())
        .as("measured_at 이 timestamptz 가 아니면 REQ-17/CLAUDE.md의 _at=시각 계약과 어긋난다(PLAN §결정 표)")
        .containsPattern(Pattern.compile("measured_at\\s+timestamptz"));
  }
}
