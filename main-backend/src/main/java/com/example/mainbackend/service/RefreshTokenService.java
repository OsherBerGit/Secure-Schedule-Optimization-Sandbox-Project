package com.example.mainbackend.service;

import com.example.mainbackend.security.CustomUserDetailsService;
import com.example.mainbackend.security.JwtUtil;
import com.example.mainbackend.dto.auth.AuthenticationResponse;
import com.example.mainbackend.entity.User;
import com.example.mainbackend.repository.UserRepository;
import com.example.mainbackend.security.JwtProperties;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import java.util.Date;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class RefreshTokenService {

    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;
    private final CustomUserDetailsService customUserDetailsService;
    private final TokenBlacklistService tokenBlacklistService;
    private final Cache<String, String> refreshTokenIps;

    public RefreshTokenService(JwtUtil jwtUtil, UserRepository userRepository, CustomUserDetailsService customUserDetailsService, TokenBlacklistService tokenBlacklistService, JwtProperties jwtProperties) {
        this.jwtUtil = jwtUtil;
        this.userRepository = userRepository;
        this.customUserDetailsService = customUserDetailsService;
        this.tokenBlacklistService = tokenBlacklistService;
        this.refreshTokenIps = Caffeine.newBuilder()
                .expireAfterWrite(jwtProperties.getRefreshTokenExpiration(), TimeUnit.MILLISECONDS)
                .maximumSize(jwtProperties.getCacheMaximumSize())
                .build();
    }

    public AuthenticationResponse refreshAccessToken(String refreshToken, String clientIP) {

        String oldJwtID = jwtUtil.extractJWTID(refreshToken);

        if (tokenBlacklistService.isTokenBlacklisted(oldJwtID))
            throw new RuntimeException("Token is blacklisted");

        String storedIP = refreshTokenIps.getIfPresent(oldJwtID);
        if (storedIP == null || !storedIP.equals(clientIP))
            throw new RuntimeException("Invalid IP address for this refresh token");

        String nationalId = jwtUtil.extractNationalId(refreshToken);
        UserDetails userDetails = customUserDetailsService.loadUserByUsername(nationalId);

        if (!jwtUtil.validateToken(refreshToken, userDetails))
            throw new RuntimeException("Invalid or expired refresh token");

        String newJwtId = UUID.randomUUID().toString();

        User user = userRepository.findByNationalId(nationalId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        Long departmentId = (user.getDepartment() != null) ? user.getDepartment().getId() : null;

        String newAccessToken = jwtUtil.generateToken(userDetails, departmentId, newJwtId);
        String newRefreshToken = jwtUtil.generateRefreshToken(userDetails, newJwtId);

        Date expiryDate = jwtUtil.extractExpiration(refreshToken);
        tokenBlacklistService.blacklistToken(oldJwtID, expiryDate, "refresh");

        storeRefreshTokenIp(newJwtId, clientIP);

        return new AuthenticationResponse(newAccessToken, newRefreshToken);
    }

    public void storeRefreshTokenIp(String jwtId, String ip) { 
        refreshTokenIps.put(jwtId, ip); 
    }
}
