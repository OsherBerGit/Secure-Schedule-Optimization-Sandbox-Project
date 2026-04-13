package com.example.mainbackend.controller;

import com.example.mainbackend.dto.auth.AuthenticationRequest;
import com.example.mainbackend.dto.auth.AuthenticationResponse;
import com.example.mainbackend.dto.auth.RefreshTokenRequest;
import com.example.mainbackend.service.AuthenticationService;
import com.example.mainbackend.service.RefreshTokenService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthenticationController {

    private final AuthenticationService authenticationService;
    private final RefreshTokenService refreshTokenService;

    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody AuthenticationRequest authenticationRequest,
                                              HttpServletRequest request) {
        try {
            String clientIP = request.getRemoteAddr();
            authenticationRequest.setIp(clientIP);
            AuthenticationResponse authResponse = authenticationService.authenticate(authenticationRequest);
            return ResponseEntity.ok(authResponse);
        } catch (AuthenticationException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("{\"error\": \"Invalid national ID or password\"}");
        }
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<?> refreshAccessToken(@Valid @RequestBody RefreshTokenRequest refreshTokenRequest,
                                                HttpServletRequest request) {
        if (request == null || refreshTokenRequest.getRefreshToken() == null || refreshTokenRequest.getRefreshToken().isEmpty())
            return ResponseEntity.badRequest().build();

        try {
            String clientIP = request.getRemoteAddr();
            refreshTokenRequest.setIp(clientIP);
            AuthenticationResponse authResponse = refreshTokenService.refreshAccessToken(refreshTokenRequest);
            return ResponseEntity.ok(authResponse);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());
        }
    }
}
