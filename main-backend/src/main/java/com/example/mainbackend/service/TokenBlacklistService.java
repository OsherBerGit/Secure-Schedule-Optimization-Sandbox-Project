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

@Service
@RequiredArgsConstructor
@Slf4j
public class TokenBlacklistService {

    private final BlacklistedTokenRepository blacklistedTokenRepository;

    @Transactional
    public void blacklistToken(String jwtId, Date expirationTime) {
        blacklistToken(jwtId, expirationTime, "logout");
    }

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

    @Transactional(readOnly = true)
    public boolean isTokenBlacklisted(String jwtId) {
        if (jwtId == null) return false;
        return blacklistedTokenRepository.existsByJwtId(jwtId);
    }

    @Transactional
    @Scheduled(fixedRate = 300_000)
    public void scheduledCleanup() {
        int deletedCount = blacklistedTokenRepository.deleteExpiredTokens(Instant.now());
        if (deletedCount > 0)
            log.info("Cleaned up {} expired blacklisted tokens", deletedCount);
    }
}
