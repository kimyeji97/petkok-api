package com.petkok.framework.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ClassPathResource;

/**
 * {@code hibernate.jdbc.time_zone} 존치 계약. 검증 계약 REQ-16-14 (PLAN-REQ-16 § 검증 계약).
 *
 * <p>⚠️ <b>이 설정은 지워도 아무 동작이 안 바뀐다</b> — {@code timestamptz} 에는 무영향이기 때문이다 (2026-08-28 Phase 0 실측:
 * {@code UTC} 와 {@code Asia/Seoul} 의 저장·조회 결과가 완전히 같았다). 그래서 "안 쓰는 설정"으로 보여 정리 대상이 되기 쉽고, <b>지운 순간이
 * 아니라 훗날 누가 {@code timestamp}(오프셋 없음) 컬럼을 추가한 순간</b> JVM 기본 TZ 의존이 되살아난다. 이 케이스가 잡는 것은 그 시차다.
 *
 * <p>텍스트 검색이 아니라 {@link YamlPropertySourceLoader} 로 읽는다 — 들여쓰기·줄바꿈이 바뀌어도 계약은 그대로여야 하고, {@code grep}
 * 은 이 주석에 적힌 같은 문자열에도 걸려 자기 자신을 근거로 삼게 된다.
 */
class HibernateTimeZonePropertyTest {

  private static final String KEY = "spring.jpa.properties.hibernate.jdbc.time_zone";

  @Test
  @DisplayName("[REQ-16-14] hibernate.jdbc.time_zone 이 UTC 로 남아 있다")
  void req_16_14_jdbcTimeZoneStaysUtc() throws IOException {
    PropertySource<?> source =
        new YamlPropertySourceLoader()
            .load("application", new ClassPathResource("application.yml"))
            .get(0);

    assertThat(source.getProperty(KEY))
        .as("timestamptz 에는 무영향이지만, timestamp 컬럼이 다시 생기면 이 한 줄이 유일한 방어선이다")
        .isEqualTo("UTC");
  }
}
