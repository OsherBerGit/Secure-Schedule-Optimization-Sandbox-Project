package com.example.mainbackend.controller;

import com.example.mainbackend.dto.auth.AuthenticationRequest;
import com.example.mainbackend.dto.auth.AuthenticationResponse;
import com.example.mainbackend.security.JwtProperties;
import com.example.mainbackend.service.AuthenticationService;
import com.example.mainbackend.service.RefreshTokenService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthenticationController {

    private final AuthenticationService authenticationService;
    private final RefreshTokenService refreshTokenService;

    private ResponseCookie createRefreshTokenCookie(String refreshToken) {
        return ResponseCookie.from("refreshToken", refreshToken)
                .httpOnly(true)
                .secure(false)
                .path("/api/auth/refresh-token")
                .maxAge(JwtProperties.REFRESH_TOKEN_EXPIRATION_TIME / 1000)
                .sameSite("Strict")
                .build();
    }

    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody AuthenticationRequest authenticationRequest, HttpServletRequest request) {
        try {
            String clientIP = request.getRemoteAddr();
            AuthenticationResponse authResponse = authenticationService.authenticate(authenticationRequest, clientIP);

            ResponseCookie cookie = createRefreshTokenCookie(authResponse.getRefreshToken());

            AuthenticationResponse bodyResponse = AuthenticationResponse.builder()
                    .accessToken(authResponse.getAccessToken())
                    .build();

            return ResponseEntity.ok()
                    .header(HttpHeaders.SET_COOKIE, cookie.toString())
                    .body(bodyResponse);

        } catch (AuthenticationException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("{\"error\": \"Invalid national ID or password\"}");
        }
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<?> refreshAccessToken(@CookieValue(name = "refreshToken", required = false) String refreshToken, HttpServletRequest request) {
        if (refreshToken == null || refreshToken.isEmpty())
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("{\"error\": \"Refresh token is missing or expired\"}");

        try {
            String clientIP = request.getRemoteAddr();
            AuthenticationResponse authResponse = refreshTokenService.refreshAccessToken(refreshToken, clientIP);

            ResponseCookie cookie = createRefreshTokenCookie(authResponse.getRefreshToken());

            AuthenticationResponse bodyResponse = AuthenticationResponse.builder()
                    .accessToken(authResponse.getAccessToken())
                    .build();

            return ResponseEntity.ok()
                    .header(HttpHeaders.SET_COOKIE, cookie.toString())
                    .body(bodyResponse);

        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("{\"error\": \"" + e.getMessage() + "\"}");
        }
    }
}
