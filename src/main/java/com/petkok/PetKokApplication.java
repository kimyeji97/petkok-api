package com.petkok;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan("com.petkok")
public class PetKokApplication {

  public static void main(String[] args) {
    SpringApplication.run(PetKokApplication.class, args);
  }
}
