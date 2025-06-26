package org.ricramiel.coopeditbackend.api.controllers;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.ricramiel.coopeditbackend.api.dtos.RoomDto;
import org.ricramiel.coopeditbackend.api.dtos.UserDto;
import org.ricramiel.coopeditbackend.api.mappers.RoomMapper;
import org.ricramiel.coopeditbackend.api.mappers.UserMapper;
import org.ricramiel.coopeditbackend.domain.models.requests.RoomCreateRequestModel;
import org.ricramiel.coopeditbackend.infrastructure.services.RoomAccessManager;
import org.ricramiel.coopeditbackend.infrastructure.services.RoomService;
import org.ricramiel.coopeditbackend.infrastructure.services.UsersService;
import org.springframework.data.repository.query.Param;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("rooms")
@RequiredArgsConstructor
@Tag(name = "Rooms")
public class RoomController {
    private final RoomService roomService;
    private final RoomMapper roomMapper;

    @PostMapping("{name}")
    public RoomDto createRoom(@PathVariable("name") @Param("name") String name) {
        return roomMapper.toDto(roomService.createRoom(new RoomCreateRequestModel(name)));
    }

    @DeleteMapping("{id}")
    @PreAuthorize("hasRole('ADMIN') OR @roomAccessManager.isOwner(#id)")
    public void deleteRoomById(@PathVariable("id") @Param("id") UUID id) {
        roomService.deleteById(id);
    }
}