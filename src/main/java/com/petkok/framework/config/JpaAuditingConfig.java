package com.petkok.framework.config;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Optional;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.auditing.DateTimeProvider;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * created_at(@CreatedDate) / updated_at(@LastModifiedDate) 자동 주입 활성화. updated_at 의 SoT 는 앱(JPA
 * Auditing) — DB 트리거를 두지 않는다.
 *
 * <p>기본 {@code DateTimeProvider}는 {@code LocalDateTime}만 반환하고, Spring Data는 이를 {@code
 * OffsetDateTime} 필드({@link com.petkok.data.common.entity.BaseCreatedEntity})로 변환하지 못해 엔티티 저장이 예외로
 * 죽는다. {@link TimeConfig}의 {@code Clock}으로 만든 {@code OffsetDateTime}을 직접 공급한다.
 */
@Configuration
@EnableJpaAuditing(dateTimeProviderRef = "auditingDateTimeProvider")
public class JpaAuditingConfig {

  @Bean
  DateTimeProvider auditingDateTimeProvider(Clock clock) {
    return () -> Optional.of(OffsetDateTime.now(clock));
  }
}
