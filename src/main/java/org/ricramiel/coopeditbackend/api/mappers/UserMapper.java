package org.ricramiel.coopeditbackend.api.mappers;

import org.ricramiel.coopeditbackend.api.dtos.UserDto;
import org.ricramiel.coopeditbackend.domain.models.entities.User;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {
    public UserDto toDto(User model) {
        return new UserDto(
                model.getId(),
                model.getName(),
                model.getEmail(),
                model.isActive(),
                model.getRoles()
        );
    }
}