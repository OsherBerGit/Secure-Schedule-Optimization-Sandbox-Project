package com.example.mainbackend.security;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.util.concurrent.TimeUnit;

public class RateLimitingFilter extends OncePerRequestFilter {

    private final RateLimitProperties rateLimitProperties;
    private final Cache<String, Bucket> buckets;

    public RateLimitingFilter(RateLimitProperties rateLimitProperties) {
        this.rateLimitProperties = rateLimitProperties;
        this.buckets = Caffeine.newBuilder()
                .expireAfterAccess(rateLimitProperties.getCacheExpirationMinutes(), TimeUnit.MINUTES)
                .maximumSize(rateLimitProperties.getCacheMaximumSize())
                .build();
    }

    @Override
    public void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws IOException, ServletException {

        String clientIP = request.getRemoteAddr();
        String uri = request.getRequestURI();

        String bucketKey = uri.contains("/api/auth/login") ? "login-" + clientIP : "api-" + clientIP;

        Bucket bucket = buckets.get(bucketKey, key -> {
            int capacity = (key.startsWith("login-")) ? 10 : rateLimitProperties.getCapacity();
            return createNewBucket(capacity, (int) rateLimitProperties.getTimeUnitMinutes());
        });

        if (bucket.tryConsume(1))
            filterChain.doFilter(request, response);
        else {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType("application/json");
            response.getWriter().write("Too many requests - please try again later.");
        }
    }

    private Bucket createNewBucket(int capacity, int minutes) {
        Refill refill = Refill.greedy(capacity, java.time.Duration.ofMinutes(minutes));
        Bandwidth limit = Bandwidth.classic(capacity, refill);
        return Bucket.builder().addLimit(limit).build();
    }
}
