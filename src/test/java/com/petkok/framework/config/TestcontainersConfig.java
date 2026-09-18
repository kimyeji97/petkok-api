package com.petkok.framework.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Testcontainers PostgreSQL 컨테이너 배선(REQ-18). {@code @Import} 하는 모든 통합 테스트가 이 클래스를 공유하므로, 동일한
 * {@code @SpringBootTest} 설정(활성 프로파일·{@code @Import} 목록)을 쓰는 테스트끼리는 Spring 테스트 컨텍스트 캐시 덕에 컨테이너가 세션
 * 전체에서 한 번만 뜬다(PLAN-REQ-18 § 결정 — "컨테이너 재사용 전략").
 *
 * <p>별도 {@code @DynamicPropertySource}가 없는 이유 — {@code @ServiceConnection}이 컨테이너의 JDBC 접속 정보를
 * {@code spring.datasource.*} 대신 자동 배선한다(Spring Boot 3.3.x 1급 지원).
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfig {

  @Bean
  @ServiceConnection
  PostgreSQLContainer<?> postgresContainer() {
    return new PostgreSQLContainer<>(DockerImageName.parse("postgres:17"));
  }
}
