package com.example.mainbackend.repository;

import com.example.mainbackend.entity.BlacklistedToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;

@Repository
public interface BlacklistedTokenRepository extends JpaRepository<BlacklistedToken, String> {

    boolean existsByJwtId(String jwtId);

    @Modifying
    @Query("DELETE FROM BlacklistedToken b WHERE b.expirationTime < :now")
    int deleteExpiredTokens(@Param("now") Instant now);

    @Query("SELECT COUNT(b) FROM BlacklistedToken b WHERE b.expirationTime < :now")
    long countExpiredTokens(@Param("now") Instant now);
}

