package com.wellness.Entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Getter
@Entity
@Table(
        name = "revoked_tokens",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_revoked_token_id",
                columnNames = "token_id"
        )
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RevokedToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "token_id", nullable = false, length = 64)
    private String tokenId;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    public RevokedToken(String tokenId, Instant expiresAt) {
        this.tokenId = tokenId;
        this.expiresAt = expiresAt;
    }
}
