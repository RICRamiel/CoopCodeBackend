package org.ricramiel.coopeditbackend.infrastructure.websocket.handler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.net.URI;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class CodeWebSocketHandler extends TextWebSocketHandler {

    private final ObjectMapper objectMapper;

    // Хранилище комнат: roomId -> RoomState
    private final Map<String, RoomState> rooms = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(@NonNull WebSocketSession session) {
        String roomId = getQueryParam(session, "room");
        String userId = getQueryParam(session, "user");

        session.getAttributes().put("roomId", roomId);

        RoomState room = rooms.computeIfAbsent(roomId, k -> new RoomState());
        room.addSession(session, userId);

        // Отправляем текущее состояние новому клиенту
        sendInitialState(session, room);
        log.info("User connected: {} to room: {}", userId, roomId);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        //todo delete!!!
        Thread.sleep(190);
        log.debug("Incoming message: {}", message.getPayload());

        JsonNode json = objectMapper.readTree(message.getPayload());
        String type = json.get("type").asText();
        String roomId = json.get("roomId").asText();
        String userId = json.get("userId").asText();

        RoomState room = rooms.get(roomId);
        if (room == null) {
            sendError(session, "ROOM_NOT_FOUND", "Room does not exist");
            return;
        }

        switch (type) {
            case "REQUEST_STATE":
                sendInitialState(session, room);
                break;

            case "APPLY_OPERATIONS":
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
        }
    }

    @Override
    public void afterConnectionClosed(@NonNull WebSocketSession session, @NonNull CloseStatus status) {
        String roomId = (String) session.getAttributes().get("roomId");
        if (roomId != null && rooms.containsKey(roomId)) {
            rooms.get(roomId).removeSession(session.getId());

            if (rooms.get(roomId).isEmpty()) {
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