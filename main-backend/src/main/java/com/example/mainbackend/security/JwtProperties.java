package com.example.mainbackend.security;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

/**
 * Configuration properties for JWT authentication.
 * Values are loaded from application.properties or environment variables.
 */
@Component
@Configuration
@ConfigurationProperties(prefix = "jwt")
@Getter
@Setter
public class JwtProperties {

    /**
     * Secret key for signing JWTs.
     * IMPORTANT: Override in production using JWT_SECRET environment variable.
     */
    private String secret = "default-dev-secret-key-change-in-production-min-32-chars";

    /**
     * Access token expiration time in milliseconds.
     * Default: 15 minutes (900000 ms)
     */
    private long accessTokenExpiration = 15 * 60 * 1000;

    /**
     * Refresh token expiration time in milliseconds.
     * Default: 7 days (604800000 ms)
     */
    private long refreshTokenExpiration = 7 * 24 * 60 * 60 * 1000L;

    // Static constants for backward compatibility
    public static final String TOKEN_PREFIX = "Bearer ";
    public static final String HEADER_STRING = "Authorization";

    // Legacy static access (deprecated - use instance methods instead)
    @Deprecated
    public static final int ACCESS_TOKEN_EXPIRATION_TIME = 15 * 60 * 1000;

    @Deprecated
    public static final int REFRESH_TOKEN_EXPIRATION_TIME = 7 * 24 * 60 * 60 * 1000;
}
