package com.example.mainbackend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling // Enable scheduled tasks (e.g., token blacklist cleanup)
@EnableConfigurationProperties // Enable @ConfigurationProperties binding
public class MainBackendApplication {
    public static void main(String[] args) {
        SpringApplication.run(MainBackendApplication.class, args);
    }
}
