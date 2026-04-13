package com.example.mainbackend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

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

    @Id
    @Column(name = "jwt_id", length = 36)
    private String jwtId;

    @Column(name = "expiration_time", nullable = false)
    private Instant expirationTime;

    @Column(name = "blacklisted_at", nullable = false)
    private Instant blacklistedAt;

    @Column(length = 100)
    private String reason;
}

