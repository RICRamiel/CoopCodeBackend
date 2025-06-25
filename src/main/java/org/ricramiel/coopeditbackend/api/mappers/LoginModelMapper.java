package org.ricramiel.coopeditbackend.api.mappers;

import org.ricramiel.coopeditbackend.api.dtos.LoginModelDto;
import org.ricramiel.coopeditbackend.domain.models.requests.LoginRequestModel;
import org.springframework.stereotype.Component;

@Component
public class LoginModelMapper {
    public LoginRequestModel toDomain(LoginModelDto model) {
        return new LoginRequestModel(
                model.getEmail(),
                model.getPassword()
        );
    }
    public LoginModelDto toDto(LoginRequestModel model) {
        return new LoginModelDto(
                model.getEmail(),
                model.getPassword()
        );
    }
}