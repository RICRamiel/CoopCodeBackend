package org.ricramiel.coopeditbackend.infrastructure.websocket.handler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.AccessLevel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ricramiel.coopeditbackend.domain.models.entities.Room;
import org.ricramiel.coopeditbackend.domain.models.enums.Role;
import org.ricramiel.coopeditbackend.domain.models.enums.RoomAction;
import org.ricramiel.coopeditbackend.infrastructure.repositories.RoomRepository;
import org.ricramiel.coopeditbackend.infrastructure.services.RoomAccessManager;
import org.ricramiel.coopeditbackend.infrastructure.services.RoomService;
import org.ricramiel.coopeditbackend.infrastructure.services.RoomStateAccessManager;
import org.ricramiel.coopeditbackend.infrastructure.websocket.common.CustomWebSocketAttributeKeys;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class CodeWebSocketHandler extends TextWebSocketHandler {
    private final RoomRepository roomRepository;
    private final RoomStateAccessManager roomAccessManager;
    private final ObjectMapper objectMapper;

    // Хранилище комнат: roomId -> RoomState
    private final Map<UUID, RoomState> rooms = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(@NonNull WebSocketSession session) {
        UUID roomId = UUID.fromString(getQueryParam(session, "room"));

        if (!roomRepository.existsById(roomId)) {
            sendError(session, "REFUSED", "No room with id " + roomId);
            return;
        }

        String userId = session.getAttributes().get(CustomWebSocketAttributeKeys.USER_ID).toString();
        Set<Role> userRoles = (Set<Role>) session.getAttributes().get(CustomWebSocketAttributeKeys.ROLES);

        WebSocketSession wrappedSession = new ConcurrentWebSocketSessionDecorator(
                session,
                5000, // timeout
                1024 * 1024 // buffer size limit
        );

        wrappedSession.getAttributes().put(CustomWebSocketAttributeKeys.ROOM_ID, roomId);

        RoomState room = rooms.computeIfAbsent(roomId, k -> new RoomState());
        room.setId(roomId);
        room.addSession(new RoomState.SessionInfo(session, userId, userRoles));

        // Отправляем текущее состояние новому клиенту
        sendInitialState(wrappedSession, room);
        log.info("User connected: {} to room: {}", userId, roomId);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        log.debug("Incoming message: {}", message.getPayload());

        JsonNode json = objectMapper.readTree(message.getPayload());
        String type = json.get("type").asText();
        String userId = session.getAttributes().get(CustomWebSocketAttributeKeys.USER_ID).toString();
        UUID roomId = (UUID) session.getAttributes().get(CustomWebSocketAttributeKeys.ROOM_ID);

        RoomState room = rooms.get(roomId);
        if (room == null) {
            sendError(session, "ROOM_NOT_FOUND", "Room does not exist");
            return;
        }

        switch (type) {
            case "REQUEST_STATE":
                if (!roomAccessManager.hasAccessTo(room, session.getId(), RoomAction.READ)) break;
                sendInitialState(session, room);
                break;

            case "APPLY_OPERATIONS":
                if (!roomAccessManager.hasAccessTo(room, session.getId(), RoomAction.WRITE)) break;
                try {
                    int clientVersion = json.get("baseVersion").asInt();
                    Operation[] clientOperations = objectMapper.treeToValue(
                            json.get("operations"),
                            Operation[].class
                    );

                    List<Operation> operationsFilledWithUserId = Arrays.asList(clientOperations);
                    operationsFilledWithUserId.forEach(operation -> {
                        operation.setUserId(userId);
                    });

                    rooms.computeIfPresent(roomId, (id, roomState) -> {
                        roomState.processOperations(
                                operationsFilledWithUserId,
                                clientVersion,
                                userId,
                                session.getId()
                        );
                        return roomState;
                    });
                } catch (Exception e) {
                    log.error("Error processing operations", e);
                    sendError(session, "PROCESSING_ERROR", "Failed to apply operations");
                }
                break;

            case "CURSOR_UPDATE":
                if (!roomAccessManager.hasAccessTo(room, session.getId(), RoomAction.READ)) break;
                int position = json.get("position").asInt();
                String color = json.get("color").asText();

                rooms.computeIfPresent(roomId, (id, roomState) -> {
                    roomState.processCursorUpdate(
                            userId,
                            session.getId(),
                            position,
                            color
                    );
                    return roomState;
                });
                break;
        }
    }

    @Override
    public void afterConnectionClosed(@NonNull WebSocketSession session, @NonNull CloseStatus status) {
        UUID roomId = (UUID) session.getAttributes().get(CustomWebSocketAttributeKeys.ROOM_ID);
        if (roomId != null && rooms.containsKey(roomId)) {
            RoomState roomState = rooms.get(roomId);
            roomState.removeSession(session.getId());

            if (roomState.isEmpty()) {
                log.info("Room {} is empty", roomId);
                //if (!roomState.getContent().isBlank()) {
                    saveRoomState(roomState);
                //}
                //else{
                    //roomRepository.deleteById(roomId);
                //}
                rooms.remove(roomId);
            }
        }
    }

    private void sendInitialState(WebSocketSession session, RoomState room) {
        try {
            ObjectNode json = JsonNodeFactory.instance.objectNode()
                    .put("type", "INITIAL_STATE")
                    .put("content", room.getContent())
                    .put("version", room.getVersion());

            session.sendMessage(new TextMessage(json.toString()));
        } catch (IOException e) {
            log.error("Error sending initial state: {}", e.getMessage());
        }
    }

    @Scheduled(fixedRate = 1000 * 10)
    public void broadcastCodeSnapshots() {
        rooms.values().forEach(RoomState::broadcastCodeSnapshot);
    }

    @Scheduled(fixedRate = 1000 * 60 * 5)
    public void saveRoomStates() {
        rooms.values().forEach(this::saveRoomState);
    }

    private void saveRoomState(@NonNull RoomState roomState) {
        log.debug("Saving room with id {}", roomState.getId());
        Room room = roomRepository.findById(roomState.getId()).orElse(null);
        if (room == null){
            rooms.remove(roomState.getId());
            return;
        }
        room.setCode(roomState.getContent());
        room.setAccessMode(roomState.getAccessMode());
        room.setName(roomState.getName());
        roomRepository.save(room);
    }

    private void sendError(WebSocketSession session, String errorType, String reason) {
        try {
            ObjectNode json = JsonNodeFactory.instance.objectNode()
                    .put("type", "ERROR")
                    .put("errorType", errorType)
                    .put("reason", reason);

            session.sendMessage(new TextMessage(json.toString()));
        } catch (IOException e) {
            log.error("Error sending error message: {}", e.getMessage());
        }
    }

    private String getQueryParam(WebSocketSession session, String name) {
        return Arrays.stream(Objects.requireNonNull(session.getUri()).getQuery().split("&")).map(param -> param.split("=")).filter(pair -> pair.length == 2 && pair[0].equals(name)).map(pair -> pair[1]).findFirst().orElse("");
    }
}