package com.petkok.global.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Configuration
public class RestTemplateConfig {

  @Bean
  public RestTemplate restTemplate() {
    RestTemplate template = new RestTemplate();
    template.getInterceptors().add(restTemplateLoggingInterceptor());
    return template;
  }

  @Bean
  public RestTemplateLoggingInterceptor restTemplateLoggingInterceptor() {
    return new RestTemplateLoggingInterceptor();
  }
}
