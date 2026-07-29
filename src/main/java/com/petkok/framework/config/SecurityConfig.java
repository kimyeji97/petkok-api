package com.petkok.framework.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.petkok.framework.exception.ErrorCode;
import com.petkok.framework.processor.filter.JwtAuthenticationFilter;
import com.petkok.framework.response.ApiResponse;
import com.petkok.framework.response.ErrorResponse;
import java.nio.charset.StandardCharsets;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {

  /**
   * access 토큰 없이 호출되는 엔드포인트만 <b>개별 경로로</b> 나열한다.
   *
   * <p>⚠️ <b>{@code /api/v1/auth/**} 같은 와일드카드를 쓰지 않는다.</b> {@code /auth/} 아래에도 인증이 필요한 엔드포인트가 있다 —
   * {@code DELETE /auth/logout} 은 Request Body 가 없어 <b>access 토큰이 유일한 사용자 식별 수단</b>이라 무인증으로 열리면 안
   * 된다. (AGENTS.md §5 · REQ-07-01~03)
   */
  private static final String[] PUBLIC_PATHS = {
    "/api/v1/auth/kakao", "/api/v1/auth/refresh", "/actuator/health"
  };

  private final JwtAuthenticationFilter jwtAuthenticationFilter;
  private final ObjectMapper objectMapper;

  public SecurityConfig(
      JwtAuthenticationFilter jwtAuthenticationFilter, ObjectMapper objectMapper) {
    this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    this.objectMapper = objectMapper;
  }

  @Bean
  SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http.csrf(AbstractHttpConfigurer::disable)
        .formLogin(AbstractHttpConfigurer::disable)
        .httpBasic(AbstractHttpConfigurer::disable)
        .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(
            auth -> auth.requestMatchers(PUBLIC_PATHS).permitAll().anyRequest().authenticated())
        .exceptionHandling(
            eh ->
                eh.authenticationEntryPoint(
                    (request, response, ex) -> {
                      response.setStatus(ErrorCode.UNAUTHORIZED.getStatus().value());
                      response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                      response.setCharacterEncoding(StandardCharsets.UTF_8.name());
                      ApiResponse<Void> body =
                          ApiResponse.error(
                              new ErrorResponse(
                                  ErrorCode.UNAUTHORIZED.getCode(),
                                  ErrorCode.UNAUTHORIZED.getMessage()));
                      response.getWriter().write(objectMapper.writeValueAsString(body));
                    }))
        .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
    return http.build();
  }
}
