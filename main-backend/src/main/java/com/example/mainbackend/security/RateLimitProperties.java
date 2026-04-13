package com.example.mainbackend.security;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "app.security.rate-limit")
@Getter
@Setter
public class RateLimitProperties {
    private int capacity = 100;
    private long timeUnitMinutes = 1;
    private long cacheExpirationMinutes = 60;
    private int cacheMaximumSize = 100000;
}
