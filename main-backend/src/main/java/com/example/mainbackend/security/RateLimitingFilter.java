package com.example.mainbackend.security;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@RequiredArgsConstructor
public class RateLimitingFilter extends OncePerRequestFilter {

    private final RateLimitProperties rateLimitProperties;
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    @Override
    public void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws IOException, ServletException {

        // Get the client's IP address
        String clientIP = request.getRemoteAddr();
        String uri = request.getRequestURI();

        // Distinct keys for Login vs General API
        String bucketKey = uri.contains("/api/auth/login") ? "login-" + clientIP : "api-" + clientIP;

        // Create a bucket for the client's IP address if it doesn't exist
        Bucket bucket = buckets.computeIfAbsent(bucketKey, key -> {
            int capacity = (key.startsWith("login-")) ? 10 : rateLimitProperties.getCapacity();
            return createNewBucket(capacity, (int) rateLimitProperties.getTimeUnitMinutes());
        });

        // Try to consume a token from the bucket
        if (bucket.tryConsume(1))
            filterChain.doFilter(request, response);
        // If the bucket is empty, return a 429 Too Many Requests response
        else {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value()); // Too Many Requests
            response.setContentType("application/json");
            response.getWriter().write("Too many requests - please try again later.");
        }
    }

    private Bucket createNewBucket(int capacity, int minutes) {
        // Refill the bucket with tokens per time unit
        Refill refill = Refill.greedy(capacity, java.time.Duration.ofMinutes(minutes));
        // Bandwidth is the main class that defines the rate limit
        Bandwidth limit = Bandwidth.classic(capacity, refill);
        return Bucket.builder().addLimit(limit).build();
    }
}
