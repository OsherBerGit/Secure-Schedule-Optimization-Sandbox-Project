package com.example.mainbackend.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class TokenBlacklistService {

    private final Map<String, Date> blacklistedTokens = new ConcurrentHashMap<>();

    // Enter a token to the blacklist
    public void blacklistToken(String jwtID, Date expirationTime) { // were token before id
        blacklistedTokens.put(jwtID, expirationTime);
    }

    // Check if a token is in the blacklist
    public boolean isTokenBlacklisted(String jwtID) {
        cleanupExpiredTokens();
        return blacklistedTokens.containsKey(jwtID);
    }

    // Remove expired tokens from the blacklist
    public void cleanupExpiredTokens() {
        Date now = new Date();
        blacklistedTokens.values().removeIf(date -> date.before(now));
    }

    @Scheduled(fixedRate = 300_000)
    public void scheduledCleanup() {
        cleanupExpiredTokens();
    }
}
