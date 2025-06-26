package org.ricramiel.coopeditbackend.infrastructure.services;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.ricramiel.coopeditbackend.domain.models.entities.Room;
import org.ricramiel.coopeditbackend.infrastructure.repositories.RoomRepository;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class RoomAccessManager {
    private final RoomRepository roomRepository;
    private final CurrentUserService currentUserService;

    public boolean isOwner(UUID roomId) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new EntityNotFoundException("Room not found with id: " + roomId));

        return room.getOwnerId().equals(currentUserService.getId());
    }
}