package com.example.mainbackend.config;

import com.example.mainbackend.service.CustomLogoutHandler;
import com.example.mainbackend.service.CustomUserDetailsService;
import com.example.mainbackend.service.TokenBlacklistService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;

import java.util.List;

@Configuration
@EnableWebSecurity(debug = true) // enable debug mode to see the security filter chain in action
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtUtil jwtUtil;
    private final CustomUserDetailsService userDetailsService;
    private final TokenBlacklistService tokenBlacklistService;
    private final CustomLogoutHandler customLogoutHandler;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        // we don't need csrf protection in jwt
        http
                .csrf(AbstractHttpConfigurer::disable)
                // add cors corsConfigurer
                .cors(cors -> {
                    // register cors configuration source, React app is running on localhost:5173
                    cors.configurationSource(request -> {
                        var corsConfig = new CorsConfiguration();
                        corsConfig.setAllowedOrigins(List.of("http://localhost:5173")); // vite dev server
                        corsConfig.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
                        corsConfig.setAllowedHeaders(List.of("*"));
                        return corsConfig;
                    });
                })


                // adding a custom JWT authentication filter
                .addFilterBefore(new JwtAuthenticationFilter(jwtUtil, userDetailsService, tokenBlacklistService),
                        UsernamePasswordAuthenticationFilter.class)

                // The SessionCreationPolicy.STATELESS setting means that the application will not create or use HTTP sessions.
                // This is a common configuration in RESTful APIs, especially when using token-based authentication like JWT (JSON Web Token).
                .sessionManagement(sess -> sess.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // Adding logout mechanism
                .logout(logout -> logout
                        .logoutUrl("/api/auth/logout")
                        .logoutSuccessHandler(customLogoutHandler) // Custom logout handler class
                        .invalidateHttpSession(true) // Invalidate session
                        .clearAuthentication(true)
                        .permitAll())

                // Configuring authorization for HTTP requests
                .authorizeHttpRequests(auth -> auth
                        // Public endpoints - ALL ENDPOINTS PUBLIC FOR DEBUGGING
                        .requestMatchers("/login", "/register", "/home").permitAll()
                        .requestMatchers("/api/**").permitAll() // ALL API endpoints public for debugging
                        .requestMatchers("/users/**").permitAll()

                        .requestMatchers("/api/protected-message-admin").hasAnyRole("ADMIN")
                        .requestMatchers("/api/protected-message").hasAnyRole("USER", "ADMIN")

                        // All other requests must be authenticated
                        .anyRequest().authenticated());

        return http.build();
    }
}
