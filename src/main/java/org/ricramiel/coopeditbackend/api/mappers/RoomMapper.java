package org.ricramiel.coopeditbackend.api.mappers;

import org.ricramiel.coopeditbackend.api.dtos.RoomDto;
import org.ricramiel.coopeditbackend.api.dtos.UserDto;
import org.ricramiel.coopeditbackend.domain.models.entities.Room;
import org.ricramiel.coopeditbackend.domain.models.entities.User;
import org.springframework.stereotype.Component;

@Component
public class RoomMapper {
    public RoomDto toDto(Room model) {
        return RoomDto.builder()
                .name(model.getName())
                .accessMode(model.getAccessMode())
                .ownerId(model.getOwnerId())
                .id(model.getId())
                .creationDate(model.getCreationDate())
                .build();
    }
}