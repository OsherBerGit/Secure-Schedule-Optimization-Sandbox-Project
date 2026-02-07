package com.example.mainbackend.repository;

import com.example.mainbackend.entity.BlacklistedToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;

/**
 * Repository for managing blacklisted JWT tokens.
 */
@Repository
public interface BlacklistedTokenRepository extends JpaRepository<BlacklistedToken, String> {

    /**
     * Check if a token is blacklisted by its JWT ID.
     *
     * @param jwtId the JWT ID (jti claim)
     * @return true if the token is blacklisted
     */
    boolean existsByJwtId(String jwtId);

    /**
     * Delete all expired tokens from the blacklist.
     * This keeps the table size manageable.
     *
     * @param now the current timestamp
     * @return number of deleted records
     */
    @Modifying
    @Query("DELETE FROM BlacklistedToken b WHERE b.expirationTime < :now")
    int deleteExpiredTokens(@Param("now") Instant now);

    /**
     * Count expired tokens (for monitoring before cleanup).
     *
     * @param now the current timestamp
     * @return count of expired tokens
     */
    @Query("SELECT COUNT(b) FROM BlacklistedToken b WHERE b.expirationTime < :now")
    long countExpiredTokens(@Param("now") Instant now);
}

