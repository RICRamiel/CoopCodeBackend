package org.ricramiel.coopeditbackend.infrastructure.repositories;

import jakarta.validation.constraints.NotNull;
import org.ricramiel.coopeditbackend.domain.models.entities.Room;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RoomRepository extends JpaRepository<Room, UUID> {
    void deleteAllByCreationDateBefore(@NotNull LocalDateTime creationDateBefore);
    List<Room> findAllByOwnerId(UUID ownerId);
}