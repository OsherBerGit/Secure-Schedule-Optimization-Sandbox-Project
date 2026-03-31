package com.example.mainbackend.security;

import com.example.mainbackend.dto.auth.AuthenticationRequest;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@Slf4j
public class JwtUtil {

    private final SecretKey key;
    private final JwtProperties jwtProperties;

    public JwtUtil(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
        // Use the secret from configuration - persists across restarts
        String secret = jwtProperties.getSecret();

        // Ensure the secret is at least 32 characters (256 bits) for HS256
        if (secret.length() < 32) {
            log.warn("JWT secret is less than 32 characters. Padding for security.");
            secret = String.format("%-32s", secret).replace(' ', 'X');
        }

        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        log.info("JwtUtil initialized with configured secret key");
    }

    private SecretKey getKey() { return this.key; } // Use the stored key

    // Generate a JWT token for a user, first time login
    public String generateToken(AuthenticationRequest authenticationRequest, UserDetails userDetails, Long departmentId, String jwtID) {
        Map<String, Object> claims = new HashMap<>();
        if (departmentId != null)
            claims.put("departmentId", departmentId);

        return Jwts.builder()
                .claims()
                .add(claims)
                .subject(userDetails.getUsername())
                .setId(jwtID)
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + jwtProperties.getAccessTokenExpiration()))
                .and()
                .claim("roles", userDetails.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)
                        .collect(Collectors.toList()))
                .claim("issuedBy", "Secure-Schedule System")
                .signWith(getKey())
                .compact();
    }

    // Generate a JWT token for a user, refresh token
    public String generateRefreshToken(AuthenticationRequest authenticationRequest, UserDetails userDetails, String jwtID) {
        Map<String, Object> claims = new HashMap<>();

        return Jwts.builder()
                .claims()
                .add(claims)
                .subject(userDetails.getUsername())
                .setId(jwtID)
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + jwtProperties.getRefreshTokenExpiration()))
                .and()
                .claim("issuedBy", "Secure-Schedule System")
                .signWith(getKey())
                .compact();
    }

    // Extract the expiration date from a JWT token and implicitly validate the token
    // This implementation implicitly validates the signature when extracting claims:
    public boolean validateToken(String token, UserDetails userDetails) {
        try {
            // extract the nationalId from the JWT token
            String nationalId = extractNationalId(token);
            // If signature verification fails, extractNationalId will throw an exception.

            // check if the nationalId extracted from the JWT token matches the nationalId in the UserDetails object and the token is not expired
            return (nationalId.equals(userDetails.getUsername()) && !isTokenExpired(token));
        } catch (Exception e) {
            // Handle the invalid signature here
            throw new RuntimeException("The token signature is invalid: " + e.getMessage());
        }
        // Other exceptions related to token parsing can also be caught here if necessary
    }

    // Extract the nationalId from a JWT token
    public String extractNationalId(String token) { return extractClaim(token, Claims::getSubject); }

    // Extract the jwtID from a JWT token
    public String extractJWTID(String token) { return extractClaim(token, Claims::getId); }

    private <T> T extractClaim(String string, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(string);
        return claimsResolver.apply(claims);
    }

    // Extract all claims from a JWT token
    private Claims extractAllClaims(String token) {
        SecretKey secretKey = (SecretKey) getKey();
        return Jwts.parser()
                .verifyWith(secretKey)
                .build().parseSignedClaims(token).getPayload();
    }

    // Check if a JWT token is expired
    private Boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date(System.currentTimeMillis()));
    }

    // Extract the expiration date from a JWT token
    public Date extractExpiration(String token) { return extractClaim(token, Claims::getExpiration); }
}