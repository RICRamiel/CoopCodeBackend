package org.ricramiel.coopeditbackend.domain.models.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.ricramiel.coopeditbackend.domain.models.entities.ids.OAuth2ProviderId;

@Entity
@Data
@Builder
@IdClass(OAuth2ProviderId.class)
@NoArgsConstructor
@AllArgsConstructor
public class OAuth2Provider {
    @Id
    private String providerUserId;

    @Id
    private String provider;

    @ManyToOne
    private User user;
}