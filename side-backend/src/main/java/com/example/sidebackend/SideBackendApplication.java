package com.example.sidebackend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Side-Backend - Sandbox Gatekeeper
 *
 * <p>This server acts as a security boundary between the main-backend and the
 * algorithm core. It is intentionally stateless: no database, no JPA, no
 * session state. Every request is self-contained.</p>
 *
 * <p>Port: 8081 (configured in application.properties)</p>
 */
@SpringBootApplication
public class SideBackendApplication {
    public static void main(String[] args) {
        SpringApplication.run(SideBackendApplication.class, args);
    }
}

