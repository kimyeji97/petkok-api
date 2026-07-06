package com.petkok.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * created_at(@CreatedDate) / updated_at(@LastModifiedDate) 자동 주입 활성화.
 * updated_at 의 SoT 는 앱(JPA Auditing) — DB 트리거를 두지 않는다.
 */
@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {
}
