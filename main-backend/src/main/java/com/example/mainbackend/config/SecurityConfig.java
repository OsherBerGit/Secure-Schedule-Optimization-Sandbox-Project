package com.example.mainbackend.config;

import com.example.mainbackend.service.CustomLogoutHandler;
import com.example.mainbackend.service.CustomUserDetailsService;
import com.example.mainbackend.service.TokenBlacklistService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Security configuration for Secure-Schedule application.
 * Implements stateless JWT authentication with security best practices.
 *
 * Security Features:
 * - Stateless JWT authentication
 * - CSRF disabled (stateless API)
 * - CORS restricted to frontend origin
 * - XSS protection via security headers
 * - BCrypt password hashing (strength 12)
 * - SQL injection prevention via JPA prepared statements
 * - Method-level security with @PreAuthorize
 */
@Configuration
@EnableWebSecurity // Set debug = true only for troubleshooting
@EnableMethodSecurity // Enable @PreAuthorize, @PostAuthorize annotations
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtUtil jwtUtil;
    private final CustomUserDetailsService userDetailsService;
    private final TokenBlacklistService tokenBlacklistService;
    private final CustomLogoutHandler customLogoutHandler;

    /**
     * Password encoder using BCrypt with strength 12.
     * Strength 12 provides a good balance between security and performance.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    /**
     * CORS configuration - restricts access to frontend origin only.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // Allow ONLY the frontend origin - no wildcards for security
        configuration.setAllowedOrigins(List.of("http://localhost:5173"));

        // Allowed HTTP methods
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));

        // Allow all headers (including Authorization)
        configuration.setAllowedHeaders(List.of("*"));

        // Allow credentials (cookies, authorization headers)
        configuration.setAllowCredentials(true);

        // Cache preflight response for 1 hour
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    /**
     * Main security filter chain configuration.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) {
        http
                // Disable CSRF for stateless JWT authentication
                .csrf(csrf -> csrf.disable())

                // Configure CORS using the bean defined above
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // Enable security headers for XSS and other protections
                .headers(headers -> headers
                        .contentSecurityPolicy(csp -> csp
                                // Allow the frontend (5173) to connect back to the API (8080)
                                .policyDirectives("default-src 'self'; connect-src 'self' http://localhost:8080; frame-ancestors 'none';"))
                        .xssProtection(xss -> {})
                        .contentTypeOptions(contentTypeOptions -> {})
                        .frameOptions(frameOptions -> frameOptions.deny())
                        .httpStrictTransportSecurity(hsts -> hsts
                                .includeSubDomains(true)
                                .maxAgeInSeconds(31536000))
                )

                // Add custom JWT authentication filter before UsernamePasswordAuthenticationFilter
                .addFilterBefore(
                        new JwtAuthenticationFilter(jwtUtil, userDetailsService, tokenBlacklistService),
                        UsernamePasswordAuthenticationFilter.class
                )

                // Stateless session management - no sessions created
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                // Logout configuration
                .logout(logout -> logout
                        .logoutUrl("/api/auth/logout")
                        .logoutSuccessHandler(customLogoutHandler)
                        .invalidateHttpSession(true)
                        .clearAuthentication(true)
                        .permitAll()
                )

                // Authorization rules
                .authorizeHttpRequests(auth -> auth
                        // ============================================================
                        // DEVELOPMENT MODE: ALL ENDPOINTS PUBLIC FOR DEBUGGING
                        // TODO: REMOVE THE LINE BELOW BEFORE PRODUCTION DEPLOYMENT
                        // ============================================================
                        .requestMatchers("/api/**").permitAll()

                        // ============================================================
                        // PRODUCTION AUTHORIZATION RULES (currently disabled)
                        // TODO: REMOVE THE permitAll() LINE ABOVE AND UNCOMMENT BELOW
                        // ============================================================

                        // Public endpoints - authentication and registration
//                        .requestMatchers(
//                                "/api/login",
//                                "/api/register",
//                                "/api/refresh-token"
//                        ).permitAll()
//
//                        // Public endpoints - health check and status
//                        .requestMatchers("/api/public/**", "/api/status").permitAll()
//
//                        // OPTIONS requests (preflight) - needed for CORS
//                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
//
//                        // Admin-only endpoints
//                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
//
//                        // User management - admin only
//                        .requestMatchers(HttpMethod.DELETE, "/api/users/**").hasRole("ADMIN")
//                        .requestMatchers(HttpMethod.POST, "/api/users").hasRole("ADMIN")
//
//                        // Task management - authenticated users
//                        .requestMatchers("/api/tasks/**").hasAnyRole("USER", "ADMIN")
//
//                        // Schedule operations - authenticated users
//                        .requestMatchers("/api/schedule/**").hasAnyRole("USER", "ADMIN")
//
//                        // All other requests require authentication
//                        .anyRequest().authenticated()
                );

        return http.build();
    }
}
