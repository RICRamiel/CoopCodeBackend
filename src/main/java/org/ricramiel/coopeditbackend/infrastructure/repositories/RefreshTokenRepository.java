package org.ricramiel.coopeditbackend.infrastructure.repositories;

import org.ricramiel.coopeditbackend.domain.models.entities.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Date;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {
    Optional<RefreshToken> findByValue(String refreshTokenValue);

    void deleteAllByExtractedExpiryDateBefore(Date extractedExpiryDateBefore);
}