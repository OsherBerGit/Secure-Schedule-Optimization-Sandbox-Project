package com.example.mainbackend.service;

import com.example.mainbackend.entity.BlacklistedToken;
import com.example.mainbackend.repository.BlacklistedTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Date;

/**
 * Service for managing blacklisted JWT tokens.
 * Uses database persistence to survive server restarts.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TokenBlacklistService {

    private final BlacklistedTokenRepository blacklistedTokenRepository;

    /**
     * Add a token to the blacklist.
     *
     * @param jwtId the JWT ID (jti claim) to blacklist
     * @param expirationTime when the original token expires
     */
    @Transactional
    public void blacklistToken(String jwtId, Date expirationTime) {
        blacklistToken(jwtId, expirationTime, "logout");
    }

    /**
     * Add a token to the blacklist with a reason.
     *
     * @param jwtId the JWT ID (jti claim) to blacklist
     * @param expirationTime when the original token expires
     * @param reason why the token was blacklisted
     */
    @Transactional
    public void blacklistToken(String jwtId, Date expirationTime, String reason) {
        if (jwtId == null || expirationTime == null) {
            log.warn("Attempted to blacklist token with null jwtId or expirationTime");
            return;
        }

        // Don't re-blacklist if already exists
        if (blacklistedTokenRepository.existsByJwtId(jwtId)) {
            log.debug("Token {} is already blacklisted", jwtId);
            return;
        }

        BlacklistedToken token = BlacklistedToken.builder()
                .jwtId(jwtId)
                .expirationTime(expirationTime.toInstant())
                .blacklistedAt(Instant.now())
                .reason(reason)
                .build();

        blacklistedTokenRepository.save(token);
        log.info("Token {} blacklisted. Reason: {}", jwtId, reason);
    }

    /**
     * Check if a token is in the blacklist.
     *
     * @param jwtId the JWT ID to check
     * @return true if the token is blacklisted
     */
    @Transactional(readOnly = true)
    public boolean isTokenBlacklisted(String jwtId) {
        if (jwtId == null)
            return false;
        return blacklistedTokenRepository.existsByJwtId(jwtId);
    }

    /**
     * Remove expired tokens from the blacklist.
     * Tokens that have expired don't need to be tracked anymore
     * since they would fail validation anyway.
     */
    @Transactional
    public void cleanupExpiredTokens() {
        int deletedCount = blacklistedTokenRepository.deleteExpiredTokens(Instant.now());
        if (deletedCount > 0)
            log.info("Cleaned up {} expired blacklisted tokens", deletedCount);
    }

    /**
     * Scheduled cleanup job - runs every 5 minutes.
     */
    @Scheduled(fixedRate = 300_000)
    public void scheduledCleanup() {
        cleanupExpiredTokens();
    }
}
