package org.ricramiel.coopeditbackend.infrastructure.services;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.ricramiel.coopeditbackend.domain.models.entities.Room;
import org.ricramiel.coopeditbackend.domain.models.enums.Role;
import org.ricramiel.coopeditbackend.domain.models.enums.RoomAccessMode;
import org.ricramiel.coopeditbackend.domain.models.enums.RoomAction;
import org.ricramiel.coopeditbackend.infrastructure.repositories.RoomRepository;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class RoomAccessManager {
    private final CurrentUserService currentUserService;
    private final RoomRepository roomRepository;

    public boolean hasAccessTo(UUID roomId, RoomAction action) {
        if (currentUserService.hasRole(Role.ADMIN)){
            return true;
        }

        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new EntityNotFoundException("Room not found with id " + roomId));

        return switch (action) {
            case JOIN -> room.getAccessMode() == RoomAccessMode.PUBLIC || room.getAccessMode() == RoomAccessMode.PUBLIC_READ || isOwner(roomId);
            case EDIT_NAME -> room.getAccessMode() == RoomAccessMode.PUBLIC || isOwner(roomId);
            case EDIT_ACCESS_MODE -> isOwner(roomId);
            case DELETE -> isOwner(roomId);
            case WRITE -> room.getAccessMode() == RoomAccessMode.PUBLIC || isOwner(roomId);
            case READ -> room.getAccessMode() == RoomAccessMode.PUBLIC || room.getAccessMode() == RoomAccessMode.PUBLIC_READ || isOwner(roomId);
            default -> false;
        };
    }

    public boolean isOwner(UUID roomId) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new EntityNotFoundException("Room not found with id: " + roomId));

        return !isCreatedByAnon(room) && room.getOwnerId().equals(currentUserService.getId());
    }

    public boolean isCreatedByAnon(Room room) {
        return room.getOwnerId() == null;
    }
}