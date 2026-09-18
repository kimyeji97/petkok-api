package com.petkok.framework.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

/**
 * Testcontainers PostgreSQL 통합의 첫 스모크 — Flyway 마이그레이션이 실제 컨테이너에 적용되는지 확인한다. 검증 계약 REQ-18-01
 * (PLAN-REQ-18 § 검증 계약).
 *
 * <p>⚠️ 이 파일은 {@code TestcontainersConfig}가 아직 없어 컴파일되지 않는다 — {@code /implement REQ-18 1}이 만든다.
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfig.class)
class FlywayMigrationContainerTest {

  @Autowired private Flyway flyway;

  @Test
  @DisplayName("[REQ-18-01] 컨테이너 기동 후 Flyway 마이그레이션이 자동 적용된다")
  void req_18_01_flywayMigrationsAppliedOnContainer() {
    assertThat(flyway.info().applied()).isNotEmpty();
  }
}
