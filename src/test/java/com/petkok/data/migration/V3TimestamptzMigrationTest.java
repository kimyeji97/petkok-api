package com.petkok.data.migration;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * {@code V3__time_to_timestamptz.sql} 의 형태 계약. 검증 계약 REQ-16-04 · 05 (PLAN-REQ-16 § 검증 계약).
 *
 * <p><b>이 두 케이스는 Phase 1 전까지 실패한다.</b> 마이그레이션 파일이 아직 없기 때문이다 — {@code
 * SecurityConfigPublicPathsTest} 와 같은 자리이고, 통과하는 가짜 테스트 대신 빨간불을 남긴다.
 *
 * <p>SQL 을 <em>파싱</em>하지 않고 텍스트로 센다. 이 레포에는 SQL 파서가 없고, 여기서 잡아야 하는 사고는 문법 오류가 아니라 <b>빠뜨린 컬럼</b>과
 * <b>빠뜨린 {@code USING} 절</b>이기 때문이다. 둘 다 문법적으로는 완전히 올바른 SQL 이라 DB 가 알려주지 않는다 — {@code USING} 이 없으면
 * Postgres 가 세션 타임존으로 조용히 해석한다(D6 기각안).
 */
class V3TimestamptzMigrationTest {

  private static final String MIGRATION = "/db/migration/V3__time_to_timestamptz.sql";

  /** 계획서 범위—포함이 열거한 19개 컬럼. */
  private static final int EXPECTED_CONVERSIONS = 19;

  /** {@code alter column <이름> [set data] type timestamptz} — 대소문자·공백 무시. */
  private static final Pattern CONVERSION =
      Pattern.compile(
          "alter\\s+column\\s+\\S+\\s+(?:set\\s+data\\s+)?type\\s+timestamptz",
          Pattern.CASE_INSENSITIVE);

  private static String sql() throws IOException {
    try (InputStream in = V3TimestamptzMigrationTest.class.getResourceAsStream(MIGRATION)) {
      assertThat(in).as("마이그레이션 파일이 클래스패스에 없다: %s (Phase 1 미착수)", MIGRATION).isNotNull();
      return new String(in.readAllBytes(), StandardCharsets.UTF_8);
    }
  }

  /** {@code alter column} 절 단위로 자른다 — 한 문장에 여러 컬럼이 묶여 있어도 각각 세어진다. */
  private static List<String> conversionClauses(String sql) {
    String flat = sql.replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    List<String> clauses = new ArrayList<>();
    Matcher m = CONVERSION.matcher(flat);
    while (m.find()) {
      int next = flat.indexOf("alter column", m.end());
      int semi = flat.indexOf(';', m.end());
      int end =
          next < 0 ? (semi < 0 ? flat.length() : semi) : (semi < 0 ? next : Math.min(next, semi));
      clauses.add(flat.substring(m.start(), end));
    }
    return clauses;
  }

  @Test
  @DisplayName("[REQ-16-04] timestamptz 로 바꾸는 컬럼이 정확히 19개다")
  void req_16_04_convertsExactlyNineteenColumns() throws IOException {
    assertThat(conversionClauses(sql()))
        .as(
            "계획서 범위—포함의 19개 컬럼(users 3 · user_social_accounts 1 · pets 3 · diary_entries 2"
                + " · feeding_logs 2 · activity_logs 2 · weight_logs 1 · shed_records 1 · photos 1"
                + " · refresh_tokens 3)")
        .hasSize(EXPECTED_CONVERSIONS);
  }

  @Test
  @DisplayName("[REQ-16-05] 모든 타입 변환에 USING ... AT TIME ZONE 'UTC' 가 붙어 있다")
  void req_16_05_everyConversionHasUsingAtTimeZoneUtc() throws IOException {
    assertThat(conversionClauses(sql()))
        .as("USING 이 없으면 Postgres 가 세션 타임존으로 해석해 배포 환경마다 결과가 달라진다 (D6 기각안)")
        .isNotEmpty()
        .allMatch(clause -> clause.contains("at time zone 'utc'"));
  }
}
