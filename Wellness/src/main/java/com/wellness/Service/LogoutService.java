package com.wellness.Service;

import com.wellness.Config.JwtProvider;
import com.wellness.Entity.RevokedToken;
import com.wellness.Repository.RevokedTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class LogoutService {

    private final JwtProvider jwtProvider;
    private final RevokedTokenRepository revokedTokenRepository;

    @Transactional
    public void logout(String token) {
        JwtProvider.ParsedToken parsedToken = jwtProvider.parseToken(token);
        revokedTokenRepository.deleteByExpiresAtBefore(Instant.now());
        if (!revokedTokenRepository.existsByTokenId(parsedToken.tokenId())) {
            revokedTokenRepository.save(new RevokedToken(
                    parsedToken.tokenId(), parsedToken.expiresAt()));
        }
    }
}
