package com.petkok;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

// proxyBeanMethods=false: @Bean 메서드가 없는 진입점이라 CGLIB 프록시 불필요.
// CGLIB 비활성화로 아래 private 생성자(HideUtilityClassConstructor 준수)와 충돌하지 않는다.
@SpringBootApplication(proxyBeanMethods = false)
@ConfigurationPropertiesScan("com.petkok")
public class PetKokApplication {

  private PetKokApplication() {}

  public static void main(String[] args) {
    SpringApplication.run(PetKokApplication.class, args);
  }
}
