package com.example.mainbackend.service;

import com.example.mainbackend.config.JwtUtil;
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
        // load the user details from the database using the nationalId
        UserDetails userDetails = customUserDetailsService.loadUserByUsername(authenticationRequest.getNationalId());

        // check if the password matches the password in the database
        if (!passwordEncoder.matches(authenticationRequest.getPassword(), userDetails.getPassword()))
            throw new AuthenticationServiceException("Invalid credentials");
        
        // Fetch User entity to get departmentId
        User user = userRepository.findByNationalId(authenticationRequest.getNationalId())
                .orElseThrow(() -> new AuthenticationServiceException("User not found"));
        Long departmentId = (user.getDepartment() != null) ? user.getDepartment().getId() : null;

        // generate a unique ID for the two tokens
        String jwtID = UUID.randomUUID().toString();

        String clientIP = authenticationRequest.getIp();
        refreshTokenService.storeRefreshTokenIp(jwtID, clientIP);

        // generate the JWT access token and refresh token
        String accessToken = jwtUtil.generateToken(authenticationRequest, userDetails, departmentId, jwtID);
        String refreshToken = jwtUtil.generateRefreshToken(authenticationRequest, userDetails, jwtID);

        // return the AuthenticationResponse object
        return new AuthenticationResponse(accessToken, refreshToken);
    }
}
