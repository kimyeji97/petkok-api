package com.petkok.framework.constant;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZoneId;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 노출·계산 타임존 상수의 계약. 검증 계약 REQ-16-11 · 16 (PLAN-REQ-16 § 검증 계약).
 *
 * <p>⚠️ <b>두 케이스는 Phase 3 전까지 실패한다.</b> {@link TimeConstant} 가 아직 없기 때문이다 — {@code
 * V3TimestamptzMigrationTest} 와 같은 자리이고, 통과하는 가짜 테스트 대신 빨간불을 남긴다.
 */
class TimeConstantTest {

  /** 프로덕션 소스만 훑는다 — {@code ArchitectureTest} 의 {@code DoNotIncludeTests} 와 같은 범위 판단이다. */
  private static final Path MAIN_SOURCES = Path.of("src", "main", "java");

  private static final String ZONE_LITERAL = "Asia/Seoul";

  /** 리터럴을 가져도 되는 유일한 파일. */
  private static final String OWNER = "TimeConstant.java";

  private static List<Path> javaSources() throws IOException {
    try (Stream<Path> paths = Files.walk(MAIN_SOURCES)) {
      return paths
          .filter(Files::isRegularFile)
          .filter(p -> p.toString().endsWith(".java"))
          .toList();
    }
  }

  private static boolean containsZoneLiteral(Path source) {
    try {
      return Files.readString(source, StandardCharsets.UTF_8).contains(ZONE_LITERAL);
    } catch (IOException e) {
      throw new IllegalStateException("소스를 읽지 못했다: " + source, e);
    }
  }

  @Test
  @DisplayName("[REQ-16-11] 노출·계산 타임존 상수는 framework/constant 에 있고 값이 Asia/Seoul 이다")
  void req_16_11_zoneConstantIsSeoul() {
    assertThat(TimeConstant.KST)
        .as("계획서 범위—포함 — \"ZoneId 상수를 framework/constant 에 한 곳\" · D4 달력 판정 기준")
        .isEqualTo(ZoneId.of(ZONE_LITERAL));
  }

  /**
   * ⚠️ <b>0 건이 "깨끗함"인지 "스캐너 고장"인지 결과만으로는 구별되지 않는다</b> (CLAUDE.md — 빈 패턴은 0건이 아니라 전건 매치). 그래서 단언 앞에
   * 검사기가 살아 있음을 두 가지로 확인한다 — 훑은 파일이 비지 않았고, {@link TimeConstant} <b>자신은 리터럴을 갖고 있다</b>.
   *
   * <p>제약·함정이 두 곳이라 적었지만 2026-08-31 실측은 <b>세 곳</b>이었다({@code LocalDateTimeUtil.ZONE_ASIA_SEOUL}
   * 누락). 사람이 읽는 "늘리지 말 것" 경고를 빨간불로 바꾸는 것이 이 케이스다.
   */
  @Test
  @DisplayName("[REQ-16-16] Asia/Seoul 리터럴이 TimeConstant 밖에 없다")
  void req_16_16_zoneLiteralIsNotScattered() throws IOException {
    List<Path> sources = javaSources();
    assertThat(sources).as("프로덕션 소스를 하나도 못 찾았다 — 스캐너가 고장났다: %s", MAIN_SOURCES).isNotEmpty();
    assertThat(sources.stream().filter(TimeConstantTest::containsZoneLiteral).toList())
        .as("역프로브 — 상수 파일 자신은 리터럴을 갖고 있어야 검사기가 살아 있다")
        .isNotEmpty();

    assertThat(
            sources.stream()
                .filter(source -> !source.endsWith(OWNER))
                .filter(TimeConstantTest::containsZoneLiteral)
                .map(Path::toString)
                .toList())
        .as("제약·함정 — \"한 곳만 바꾸면 조용히 갈린다\" · \"늘리지 말 것\"")
        .isEmpty();
  }
}
