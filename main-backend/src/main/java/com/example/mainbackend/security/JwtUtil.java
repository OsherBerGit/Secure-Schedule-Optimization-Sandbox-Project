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
        String secret = jwtProperties.getSecret();

        // Ensure the secret is at least 32 characters (256 bits) for HS256
        if (secret.length() < 32) {
            log.warn("JWT secret is less than 32 characters. Padding for security.");
            secret = String.format("%-32s", secret).replace(' ', 'X');
        }

        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        log.info("JwtUtil initialized with configured secret key");
    }

    private SecretKey getKey() { return this.key; }

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

    public boolean validateToken(String token, UserDetails userDetails) {
        try {
            String nationalId = extractNationalId(token);
            return (nationalId.equals(userDetails.getUsername()) && !isTokenExpired(token));
        } catch (Exception e) {
            throw new RuntimeException("The token signature is invalid: " + e.getMessage());
        }
        // Other exceptions related to token parsing can also be caught here if necessary
    }

    public String extractNationalId(String token) { return extractClaim(token, Claims::getSubject); }

    public String extractJWTID(String token) { return extractClaim(token, Claims::getId); }

    private <T> T extractClaim(String string, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(string);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        SecretKey secretKey = (SecretKey) getKey();
        return Jwts.parser()
                .verifyWith(secretKey)
                .build().parseSignedClaims(token).getPayload();
    }

    private Boolean isTokenExpired(String token) { return extractExpiration(token).before(new Date(System.currentTimeMillis()));}

    public Date extractExpiration(String token) { return extractClaim(token, Claims::getExpiration); }
}