package com.example.mainbackend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * Entity to persist blacklisted JWT tokens in the database.
 * Ensures tokens remain invalidated even after server restart.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "blacklisted_tokens", indexes = {
    @Index(name = "idx_blacklisted_expiration", columnList = "expiration_time")
})
public class BlacklistedToken {

    /**
     * The JWT ID (jti claim) - used as primary key since it's already unique.
     */
    @Id
    @Column(name = "jwt_id", length = 36)
    private String jwtId;

    /**
     * When the original token expires.
     * Used for cleanup - no need to keep records for expired tokens.
     */
    @Column(name = "expiration_time", nullable = false)
    private Instant expirationTime;

    /**
     * When the token was blacklisted.
     * Useful for auditing and debugging.
     */
    @Column(name = "blacklisted_at", nullable = false)
    private Instant blacklistedAt;

    /**
     * Optional reason for blacklisting (logout, security concern, etc.)
     */
    @Column(length = 100)
    private String reason;
}

