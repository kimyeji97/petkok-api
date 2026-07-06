package com.madangido;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan("com.madangido")
public class MadangidoApplication {

    public static void main(String[] args) {
        SpringApplication.run(MadangidoApplication.class, args);
    }
}
