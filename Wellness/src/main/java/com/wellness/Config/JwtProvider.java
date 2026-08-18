package com.wellness.Config;

import com.wellness.Dto.AuthenticatedUser;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Date;
import java.util.HexFormat;
import java.util.UUID;

@Component
public class JwtProvider {

    private final SecretKey key;
    private final long expirationMs;

    public JwtProvider(
            @Value("${jwt.secret:not-today-local-secret-key-2026-must-be-long-enough}") String secret,
            @Value("${jwt.expiration-ms:2592000000}") long expirationMs
    ) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
    }

    public String createAccessToken(Long userId, String email) {

        Instant now = Instant.now();

        return Jwts.builder()
                .subject(String.valueOf(userId))
                .id(UUID.randomUUID().toString())
                .claim("email", email)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusMillis(expirationMs)))
                .signWith(key)
                .compact();
    }

    public AuthenticatedUser parse(String token) throws JwtException {
        return parseToken(token).authenticatedUser();
    }

    public ParsedToken parseToken(String token) throws JwtException {
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        Long userId = Long.valueOf(claims.getSubject());
        String email = claims.get("email", String.class);

        String tokenId = claims.getId() != null ? claims.getId() : fingerprint(token);
        return new ParsedToken(
                new AuthenticatedUser(userId, email),
                tokenId,
                claims.getExpiration().toInstant()
        );
    }

    private String fingerprint(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (Exception exception) {
            throw new IllegalStateException("토큰 식별자를 생성하지 못했습니다.", exception);
        }
    }

    public record ParsedToken(
            AuthenticatedUser authenticatedUser,
            String tokenId,
            Instant expiresAt
    ) {
    }
}
