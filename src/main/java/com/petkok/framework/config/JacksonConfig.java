package com.petkok.framework.config;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.petkok.framework.constant.TimeConstant;
import com.petkok.framework.processor.converter.OffsetDateTimeDeserializer;
import java.time.OffsetDateTime;
import java.util.TimeZone;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** 응답/요청 필드는 전역 snake_case. (DTO 필드는 camelCase 로 두고 직렬화 시 변환) */
@Configuration
public class JacksonConfig {

  /**
   * ⚠️ <b>{@code timeZone(...)} 을 빼면 응답이 {@code Z} 로 나간다.</b> {@code timestamptz} 는 원래 오프셋을 저장하지 않아
   * DB 에서 읽은 값은 항상 오프셋이 {@code Z} 이고, Jackson 은 <b>타임존을 명시했을 때만</b> 그 존으로 렌더한다({@code
   * hasExplicitTimeZone}). 기본값은 UTC 이며 <b>JVM 기본 TZ 를 따라가지 않는다</b> — 즉 이 한 줄이 없으면 어느 환경에서든 {@code
   * Z} 다 (2026-08-28 Phase 0 실측).
   *
   * <p>{@code WRITE_DATES_AS_TIMESTAMPS}·{@code ADJUST_DATES_TO_CONTEXT_TIME_ZONE} 은 손대지 않는다 —
   * 프로브에서 네 조합을 재 본 결과 이 한 줄 외에는 출력이 달라지지 않았다. 반대로 {@code WRITE_DATES_WITH_CONTEXT_TIME_ZONE} 은
   * <b>켜 둬야 한다</b>(기본 on). 끄면 다시 {@code Z} 가 된다.
   */
  @Bean
  Jackson2ObjectMapperBuilderCustomizer jacksonCustomizer() {
    return builder ->
        builder
            .propertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
            .timeZone(TimeZone.getTimeZone(TimeConstant.KST))
            .deserializerByType(OffsetDateTime.class, new OffsetDateTimeDeserializer());
  }
}
