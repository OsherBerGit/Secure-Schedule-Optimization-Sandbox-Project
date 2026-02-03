package com.example.mainbackend.service;

import com.example.mainbackend.config.JwtUtil;
import com.example.mainbackend.dto.AuthenticationRequest;
import com.example.mainbackend.dto.AuthenticationResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;
    private final CustomUserDetailsService customUserDetailsService;
    private final RefreshTokenService refreshTokenService;

    public AuthenticationResponse authenticate(AuthenticationRequest authenticationRequest) {
        // load the user details from the database using the username by calling the loadUserByUsername() method
        UserDetails userDetails = customUserDetailsService.loadUserByUsername(authenticationRequest.getTeudatZehut());

        // check if the password matches the password in the database
        if (!passwordEncoder.matches(authenticationRequest.getPassword(), userDetails.getPassword())) {
            throw new AuthenticationServiceException("Invalid credentials");
        }

        // generate a unique ID for the two tokens
        String jwtID = UUID.randomUUID().toString();

        String clientIP = authenticationRequest.getIp();
        refreshTokenService.storeRefreshTokenIp(jwtID, clientIP);

        // generate the JWT access token and refresh token
        String accessToken = jwtUtil.generateToken(authenticationRequest, userDetails, jwtID);
        String refreshToken = jwtUtil.generateRefreshToken(authenticationRequest, userDetails, jwtID);

        // return the AuthenticationResponse object
        return new AuthenticationResponse(accessToken, refreshToken);
    }
}
