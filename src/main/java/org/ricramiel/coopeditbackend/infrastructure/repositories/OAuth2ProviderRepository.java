package org.ricramiel.coopeditbackend.infrastructure.repositories;

import lombok.NonNull;
import org.ricramiel.coopeditbackend.domain.models.entities.OAuth2Provider;
import org.ricramiel.coopeditbackend.domain.models.entities.ids.OAuth2ProviderId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface OAuth2ProviderRepository extends JpaRepository<OAuth2Provider, OAuth2ProviderId> {
}