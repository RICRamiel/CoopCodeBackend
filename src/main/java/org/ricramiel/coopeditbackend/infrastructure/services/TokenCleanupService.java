package org.ricramiel.coopeditbackend.infrastructure.services;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ricramiel.coopeditbackend.infrastructure.repositories.RefreshTokenRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.Date;

@Slf4j
@Service
@AllArgsConstructor
public class TokenCleanupService {

    private final RefreshTokenRepository tokenRepository;

    @Scheduled(cron = "0 0 3 * * ?")
    public void cleanupExpiredTokens() {
        log.info("Starting expired token cleanup");

        Date now = new Date();
        tokenRepository.deleteAllByExtractedExpiryDateBefore(now);

        log.info("Token cleanup completed");
    }
}