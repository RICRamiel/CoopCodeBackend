package org.ricramiel.coopeditbackend.infrastructure.services;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.ricramiel.coopeditbackend.domain.models.entities.Room;
import org.ricramiel.coopeditbackend.domain.models.enums.Role;
import org.ricramiel.coopeditbackend.domain.models.enums.RoomAccessMode;
import org.ricramiel.coopeditbackend.domain.models.enums.RoomAction;
import org.ricramiel.coopeditbackend.infrastructure.repositories.RoomRepository;
import org.ricramiel.coopeditbackend.infrastructure.websocket.handler.RoomState;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class RoomStateAccessManager {
    public boolean hasAccessTo(RoomState room, String sessionId, RoomAction action) {
        RoomState.SessionInfo info = room.getSessions().get(sessionId);

        //if (info.userRoles.contains(Role.ADMIN)) {
        //    return true;
        //}

        boolean isOwner = isOwner(room, info.userId);

        return switch (action) {
            case JOIN -> room.getAccessMode() == RoomAccessMode.PUBLIC || room.getAccessMode() == RoomAccessMode.PUBLIC_READ || isOwner;
            case EDIT_NAME -> room.getAccessMode() == RoomAccessMode.PUBLIC || isOwner;
            case EDIT_ACCESS_MODE -> isOwner;
            case DELETE -> isOwner;
            case WRITE -> room.getAccessMode() == RoomAccessMode.PUBLIC || isOwner;
            case READ -> room.getAccessMode() == RoomAccessMode.PUBLIC || room.getAccessMode() == RoomAccessMode.PUBLIC_READ || isOwner;
            default -> false;
        };
    }

    private boolean isOwner(RoomState room, String userId) {
        return !isCreatedByAnon(room) && room.getOwnerId().toString().equals(userId);
    }

    private boolean isCreatedByAnon(RoomState room) {
        return room.getOwnerId() == null;
    }
}