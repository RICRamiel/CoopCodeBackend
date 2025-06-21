package org.ricramiel.coopeditbackend.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CodeWebSocketHandler extends TextWebSocketHandler {

    // Хранилище комнат: roomId -> (sessionId -> session)
    private final Map<String, Map<String, WebSocketSession>> rooms = new ConcurrentHashMap<>();

    // Текущий код для каждой комнаты
    private final Map<String, String> roomCode = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String roomId = getQueryParam(session, "room");
        String userId = getQueryParam(session, "user");

        rooms.computeIfAbsent(roomId, k -> new ConcurrentHashMap<>())
                .put(session.getId(), session);

        // Отправляем текущее состояние новому участнику
        if (roomCode.containsKey(roomId)) {
            sendMessage(session, "INITIAL_STATE", roomCode.get(roomId), userId, roomId);
        } else {
            roomCode.put(roomId, "");
        }

        System.out.println("User connected: " + userId + " to room: " + roomId);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        ObjectMapper mapper = new ObjectMapper();
        JsonNode json = mapper.readTree(payload);

        String type = json.get("type").asText();
        String roomId = json.get("roomId").asText();
        String userId = json.get("userId").asText();
        String content = json.get("content").asText();

        switch (type) {
            case "CODE_UPDATE":
                roomCode.put(roomId, content);
                broadcast(roomId, "CODE_UPDATE", content, userId, session.getId());
                break;
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String roomId = getQueryParam(session, "room");

        if (rooms.containsKey(roomId)) {
            rooms.get(roomId).remove(session.getId());

            if (rooms.get(roomId).isEmpty()) {
                rooms.remove(roomId);
                roomCode.remove(roomId);
            }
        }
    }

    private void broadcast(String roomId, String type, String content, String userId, String excludeSessionId) {
        if (!rooms.containsKey(roomId)) return;

        rooms.get(roomId).forEach((sessionId, session) -> {
            if (!sessionId.equals(excludeSessionId) && session.isOpen()) {
                sendMessage(session, type, content, userId, roomId);
            }
        });
    }

    private void sendMessage(WebSocketSession session, String type, String content, String userId, String roomId) {
        try {
            ObjectNode json = JsonNodeFactory.instance.objectNode()
                    .put("type", type)
                    .put("roomId", roomId)
                    .put("userId", userId)
                    .put("content", content);

            session.sendMessage(new TextMessage(json.toString()));
        } catch (IOException e) {
            System.err.println("Error sending message: " + e.getMessage());
        }
    }

    private String getQueryParam(WebSocketSession session, String name) {
        return Arrays.stream(session.getUri().getQuery().split("&"))
                .map(param -> param.split("="))
                .filter(pair -> pair.length == 2 && pair[0].equals(name))
                .map(pair -> pair[1])
                .findFirst()
                .orElse("");
    }
}
