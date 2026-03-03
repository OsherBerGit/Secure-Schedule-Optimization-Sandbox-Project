package com.example.algorithm;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point for the Algorithm side-backend REST service.
 *
 * Runs on port 8081 (see application.properties).
 * Completely stateless — no database, no JPA, no security layer.
 * Receives scheduling requests from main-backend via POST /api/v1/algo/schedule.
 */
@SpringBootApplication
public class SideBackendApplication {
    public static void main(String[] args) {
        SpringApplication.run(SideBackendApplication.class, args);
    }
}

