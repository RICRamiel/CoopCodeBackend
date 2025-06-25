package org.ricramiel.coopeditbackend.api.mappers;

import org.ricramiel.coopeditbackend.api.dtos.JwtModelDto;
import org.ricramiel.coopeditbackend.domain.models.responses.JwtModel;
import org.springframework.stereotype.Component;

@Component
public class JwtModelMapper {
    public JwtModel toDomain(JwtModelDto model) {
        return new JwtModel(
                model.getAccessToken(),
                model.getRefreshToken()
        );
    }
    public JwtModelDto toDto(JwtModel model) {
        return new JwtModelDto(
                model.getAccessToken(),
                model.getRefreshToken()
        );
    }
}
