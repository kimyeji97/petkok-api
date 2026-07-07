package com.petkok.global.config;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** 응답/요청 필드는 전역 snake_case. (DTO 필드는 camelCase 로 두고 직렬화 시 변환) */
@Configuration
public class JacksonConfig {

  @Bean
  Jackson2ObjectMapperBuilderCustomizer jacksonCustomizer() {
    return builder -> builder.propertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
  }
}
