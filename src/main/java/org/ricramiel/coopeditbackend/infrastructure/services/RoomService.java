package org.ricramiel.coopeditbackend.infrastructure.services;

import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ricramiel.coopeditbackend.api.dtos.RoomDto;
import org.ricramiel.coopeditbackend.domain.models.entities.Room;
import org.ricramiel.coopeditbackend.domain.models.enums.RoomAccessMode;
import org.ricramiel.coopeditbackend.domain.models.requests.RoomCreateRequestModel;
import org.ricramiel.coopeditbackend.domain.models.requests.RoomEditRequestModel;
import org.ricramiel.coopeditbackend.infrastructure.repositories.RoomRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Validated
@Slf4j
@RequiredArgsConstructor
public class RoomService {
    private final RoomRepository roomRepository;
    private final CurrentUserService currentUserService;

    @Value("${app.room.keep-alive-in-min}")
    private long roomKeepAliveInMin;

    public Room createRoom(@NotNull @Valid RoomCreateRequestModel model) {
        Room room = Room.builder()
                .owner(currentUserService.getUser())
                .name(model.getName())
                .creationDate(LocalDateTime.now())
                .code("")
                .accessMode(RoomAccessMode.PUBLIC)
                .build();
        return roomRepository.save(room);
    }

    public Room editRoom(@NotNull @Valid RoomEditRequestModel model) {
        Room room = roomRepository.findById(model.getId()).orElseThrow(() -> new EntityNotFoundException("Room not found with id: " + model.getId()));

        room.setName(model.getName());

        return roomRepository.save(room);
    }

    public Room changeRoomAccessMode(@NotNull UUID id, @NotNull RoomAccessMode mode) {
        Room room = roomRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Room not found with id: " + id));

        room.setAccessMode(mode);

        return roomRepository.save(room);
    }

    public void deleteById(@NotNull UUID id) {
        roomRepository.deleteById(id);
    }

    @Scheduled(fixedRate = 1000 * 60 * 60)
    public void clearOldRooms() {
        roomRepository.deleteAllByCreationDateBefore(LocalDateTime.now().minusMinutes(roomKeepAliveInMin));
    }

    public List<RoomDto> getAllRoomsByOwner(UUID ownerId) {
        List<Room> rooms = roomRepository.findAllByOwnerId(ownerId);
        return rooms.stream().map(room ->
                new RoomDto(room.getId(), room.getOwnerId(), room.getName(), room.getCreationDate(), room.getAccessMode()))
                .toList();
    }
}
