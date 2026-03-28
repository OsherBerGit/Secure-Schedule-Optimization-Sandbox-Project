package com.example.mainbackend.service;

import com.example.mainbackend.config.JwtUtil;
import com.example.mainbackend.dto.auth.AuthenticationResponse;
import com.example.mainbackend.dto.auth.RefreshTokenRequest;
import com.example.mainbackend.entity.User;
import com.example.mainbackend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;
    private final CustomUserDetailsService customUserDetailsService;
    private final TokenBlacklistService tokenBlacklistService;

    private final Map<String, String> refreshTokenIps = new ConcurrentHashMap<>();

    public AuthenticationResponse refreshAccessToken(RefreshTokenRequest refreshTokenRequest) {

        String refreshToken = refreshTokenRequest.getRefreshToken();

        // get the id from refresh token for the new access token
        String jwtID = jwtUtil.extractJWTID(refreshToken);

        // check if the refresh token's id is blacklisted
        if (tokenBlacklistService.isTokenBlacklisted(jwtID))
            throw new RuntimeException("Token is blacklisted");

        // load the user details from the refresh token
        String nationalId = jwtUtil.extractNationalId(refreshToken);
        UserDetails userDetails = customUserDetailsService.loadUserByUsername(nationalId);

        String clientIP = refreshTokenRequest.getIp();
        String storedIP = refreshTokenIps.get(jwtID);
        if (storedIP == null || !storedIP.equals(clientIP))
            throw new RuntimeException("Invalid IP address for this refresh token");

        // check if the refresh token is valid
        if (!jwtUtil.validateToken(refreshToken, userDetails))
            throw new RuntimeException("Invalid or expired refresh token");

        User user = userRepository.findByNationalId(nationalId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Long departmentId = (user.getDepartment() != null) ? user.getDepartment().getId() : null;

        // create a new access token
        String newAccessToken = jwtUtil.generateToken(null, userDetails, departmentId, jwtID);
        String newRefreshToken = jwtUtil.generateRefreshToken(null, userDetails, jwtID);

        // returns the new access token along with the refresh token
        return new AuthenticationResponse(newAccessToken, newRefreshToken);
    }

    public void storeRefreshTokenIp(String jwtId, String ip) { refreshTokenIps.put(jwtId, ip); }
}
