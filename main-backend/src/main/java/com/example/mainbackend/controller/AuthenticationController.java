package com.example.mainbackend.controller;

import com.example.mainbackend.dto.AuthenticationRequest;
import com.example.mainbackend.dto.AuthenticationResponse;
import com.example.mainbackend.dto.RefreshTokenRequest;
import com.example.mainbackend.service.AuthenticationService;
import com.example.mainbackend.service.RefreshTokenService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class AuthenticationController {

    private final AuthenticationService authenticationService;
    private final RefreshTokenService refreshTokenService;

    // The authenticateUser() method takes in an AuthenticationRequest object, which contains the username and password.
    // The method returns an AuthenticationResponse object, which contains the JWT and refresh token, and the user's roles.
    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@RequestBody AuthenticationRequest authenticationRequest,
                                              HttpServletRequest request) {
        try {
            String clientIP = request.getRemoteAddr();
            authenticationRequest.setIp(clientIP);
            AuthenticationResponse authResponse = authenticationService.authenticate(authenticationRequest);
            return ResponseEntity.ok(authResponse);
        } catch (AuthenticationServiceException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());
        }
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<?> refreshAccessToken(@RequestBody RefreshTokenRequest refreshTokenRequest,
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
