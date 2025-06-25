package org.ricramiel.coopeditbackend.api.mappers;

import org.ricramiel.coopeditbackend.api.dtos.RegisterModelDto;
import org.ricramiel.coopeditbackend.domain.models.requests.RegisterRequestModel;
import org.springframework.stereotype.Component;

@Component
public class RegisterModelMapper {
    public RegisterRequestModel toDomain(RegisterModelDto model) {
        return new RegisterRequestModel(
                model.getName(),
                model.getEmail(),
                model.getPassword()
        );
    }
    public RegisterModelDto toDto(RegisterRequestModel model) {
        return new RegisterModelDto(
                model.getName(),
                model.getEmail(),
                model.getPassword()
        );
    }
}