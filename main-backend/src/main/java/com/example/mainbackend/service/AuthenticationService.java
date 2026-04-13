package com.example.mainbackend.service;

import com.example.mainbackend.security.CustomUserDetailsService;
import com.example.mainbackend.security.JwtUtil;
import com.example.mainbackend.dto.auth.AuthenticationRequest;
import com.example.mainbackend.dto.auth.AuthenticationResponse;
import com.example.mainbackend.entity.User;
import com.example.mainbackend.repository.UserRepository;
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
    private final UserRepository userRepository;

    public AuthenticationResponse authenticate(AuthenticationRequest authenticationRequest) {
        UserDetails userDetails = customUserDetailsService.loadUserByUsername(authenticationRequest.getNationalId());

        if (!passwordEncoder.matches(authenticationRequest.getPassword(), userDetails.getPassword()))
            throw new AuthenticationServiceException("Invalid credentials");

        User user = userRepository.findByNationalId(authenticationRequest.getNationalId())
                .orElseThrow(() -> new AuthenticationServiceException("User not found"));
        Long departmentId = (user.getDepartment() != null) ? user.getDepartment().getId() : null;

        String jwtID = UUID.randomUUID().toString();
        String clientIP = authenticationRequest.getIp();
        refreshTokenService.storeRefreshTokenIp(jwtID, clientIP);

        String accessToken = jwtUtil.generateToken(authenticationRequest, userDetails, departmentId, jwtID);
        String refreshToken = jwtUtil.generateRefreshToken(authenticationRequest, userDetails, jwtID);

        return new AuthenticationResponse(accessToken, refreshToken);
    }
}
