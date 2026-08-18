package com.wellness.Repository;

import com.wellness.Entity.RevokedToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;

public interface RevokedTokenRepository extends JpaRepository<RevokedToken, Long> {

    boolean existsByTokenId(String tokenId);

    void deleteByExpiresAtBefore(Instant instant);
}
