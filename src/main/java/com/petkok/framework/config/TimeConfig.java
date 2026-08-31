package com.petkok.framework.config;

import com.petkok.framework.constant.TimeConstant;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * {@code now} 획득 경로 (REQ-16 D5). <b>시각은 주입받는다 — 직접 {@code now()} 를 부르지 않는다.</b>
 *
 * <p>{@code LocalDateTime.now()} 류의 무인자 호출은 <b>JVM 기본 타임존에 암묵 의존</b>한다. 배포 환경에 {@code TZ} 가 없으면 값이
 * 9시간 어긋난 채 <b>에러 없이</b> 저장된다. 검증 계약 REQ-16-10 이 그 호출을 {@code business}·{@code framework} 에서 금지하고,
 * 이 빈이 대체 경로다.
 *
 * <p>부수 이득으로 <b>테스트에서 시각을 고정</b>할 수 있다 — {@code Clock.fixed(...)} 를 주입하면 만료 경계가 실행 시각과 무관하게 재현된다
 * (REQ-16-12 · 17).
 */
@Configuration
public class TimeConfig {

  @Bean
  Clock clock() {
    return Clock.system(TimeConstant.KST);
  }
}
