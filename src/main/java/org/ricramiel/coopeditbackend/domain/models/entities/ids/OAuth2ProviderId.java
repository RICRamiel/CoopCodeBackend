package org.ricramiel.coopeditbackend.domain.models.entities.ids;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OAuth2ProviderId {
    private String providerUserId;
    private String provider;
}