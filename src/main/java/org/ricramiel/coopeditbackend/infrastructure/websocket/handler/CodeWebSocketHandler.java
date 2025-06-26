package org.ricramiel.coopeditbackend.infrastructure.websocket.handler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ricramiel.coopeditbackend.domain.models.enums.CodeChangeType;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.ricramiel.coopeditbackend.common.util.diff_match_patch;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class CodeWebSocketHandler extends TextWebSocketHandler {

    private final diff_match_patch dmp = new diff_match_patch();
    private final ObjectMapper objectMapper;
    // Хранилище комнат: roomId -> (sessionId -> session)
    private final Map<String, Map<String, WebSocketSession>> rooms = new ConcurrentHashMap<>();

    // Текущий код для каждой комнаты
    private final Map<String, String> roomCode = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(@NonNull WebSocketSession session) {
        String roomId = getQueryParam(session, "room");
        System.out.println(session);
        String userId = getQueryParam(session, "user");

        session.getAttributes().put("roomId", roomId);

        rooms.computeIfAbsent(roomId, k -> new ConcurrentHashMap<>()).put(session.getId(), session);

        //JsonNode roomCodeNode = JsonNodeFactory.instance.objectNode().put("code", roomCode.get(roomId));

        if (roomCode.containsKey(roomId)) {
            System.out.println(rooms.get(roomId));
            sendMessage(session, "INITIAL_STATE", roomCode.get(roomId));
        } else {
            roomCode.put(roomId, "");
        }

        log.info("User connected: {} to room: {}", userId, roomId);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        JsonNode json = objectMapper.readTree(payload);

        //CodeChangeType type = CodeChangeType.valueOf(json.get("type").asText());
        String type = json.get("type").asText();
        String roomId = json.get("roomId").asText();
        String userId = json.get("userId").asText();
        JsonNode contentJson = json.get("content");

        switch (type) {
            case "REQUEST_STATE":
                sendMessage(session, "INITIAL_STATE", roomCode.get(roomId));
                break;

            case "APPLY_PATCH":
                String patchText = json.get("patch").asText();
                System.out.println("Received patch from user " + userId + " in room " + roomId);

                // Применяем патч к текущему документу комнаты
                String currentContent = roomCode.get(roomId);
                List<diff_match_patch.Patch> patches = dmp.patch_fromText(patchText);
                Object[] result = dmp.patch_apply((LinkedList<diff_match_patch.Patch>) patches, currentContent);
                String newContent = (String) result[0];
                boolean[] applied = (boolean[]) result[1];

                // Проверяем успешность применения
                boolean success = true;
                for (boolean b : applied) {
                    if (!b) {
                        success = false;
                        break;
                    }
                }

                if (success) {
                    // Обновляем документ комнаты
                    roomCode.put(roomId, newContent);

                    // Рассылаем патч всем участникам комнаты, кроме отправителя
                    broadcastPatch(roomId, patchText, userId, session.getId());
                } else {
                    // Отправляем ошибку клиенту
                    sendError(session, "PATCH_FAILED", "Failed to apply patch");
                }
                break;
//            case CODE_REMOVE: {
//                int length = contentJson.get("length").asInt();
//                int index = contentJson.get("index").asInt();
//
//                String code = roomCode.get(roomId);
//                StringBuilder sb = new StringBuilder(code);
//                sb.replace(index, index + length, "");
//
//                roomCode.put(roomId, sb.toString());
//
//                broadcast(roomId, CodeChangeType.CODE_REMOVE, contentJson, userId, session.getId());
//                break;
//            }
//            case CODE_ADD: {
//                String codeToAdd = contentJson.get("code").asText();
//                int index = json.get("index").asInt();
//
//                String code = roomCode.get(roomId);
//                StringBuilder sb = new StringBuilder(code);
//                sb.insert(index, codeToAdd);
//
//                roomCode.put(roomId, sb.toString());
//
//                broadcast(roomId, CodeChangeType.CODE_ADD, contentJson, userId, session.getId());
//                break;
//            }
//            case CODE_UPDATE: {
//                String newCode = contentJson.get("code").asText();
//
//                roomCode.put(roomId, newCode);
//
//                broadcast(roomId, CodeChangeType.CODE_UPDATE, contentJson, userId, session.getId());
//                break;
//            }
        }
    }

    @Override
    public void afterConnectionClosed(@NonNull WebSocketSession session, @NonNull CloseStatus status) {
        String roomId = getQueryParam(session, "room");

        Map<String, WebSocketSession> room = rooms.get(roomId);

        if (room != null) {
            room.remove(session.getId());

            if (room.isEmpty()) {
                rooms.remove(roomId);
                roomCode.remove(roomId);
            }
        }
    }

//    private void broadcast(String roomId, CodeChangeType type, JsonNode content, String userId, String excludeSessionId) {
//        if (!rooms.containsKey(roomId)) return;
//
//        rooms.get(roomId).forEach((sessionId, session) -> {
//            if (!sessionId.equals(excludeSessionId) && session.isOpen()) {
//                sendMessage(session, type, content, userId, roomId);
//            }
//        });
//    }
//
//    private void sendMessage(WebSocketSession session, CodeChangeType type, JsonNode content, String userId, String roomId) {
//        try {
//            ObjectNode json = JsonNodeFactory.instance.objectNode().put("type", type.toString()).put("roomId", roomId).put("userId", userId).put("content", content.asText());
//
//            session.sendMessage(new TextMessage(json.toString()));
//        } catch (IOException e) {
//            log.error("Error sending message: {}", e.getMessage());
//        }
//    }

    private void broadcastPatch(String roomId, String patchText, String userId, String excludeSessionId) {
        Set<WebSocketSession> sessions = new HashSet<WebSocketSession>(rooms.get(roomId).values());

        sessions.stream().filter(session -> session.isOpen() && !session.getId().equals(excludeSessionId)).forEach(session -> {
            try {
                ObjectNode json = JsonNodeFactory.instance.objectNode()
                        .put("type", "PATCH")
                        .put("patch", patchText)
                        .put("userId", userId);
                WebSocketSession roomSession = rooms.get(roomId).get(session.getId());
                synchronized (roomSession) {
                    session.sendMessage(new TextMessage(json.toString()));
                }
            } catch (IOException e) {
                System.err.println("Error broadcasting patch: " + e.getMessage());
            }
        });
    }

    private void sendMessage(WebSocketSession session, String type, String content) {
        try {
            ObjectNode json = JsonNodeFactory.instance.objectNode().put("type", type).put("content", content);
            session.sendMessage(new TextMessage(json.toString()));
        } catch (IOException e) {
            System.err.println("Error sending message: " + e.getMessage());
        }
    }

    private void sendError(WebSocketSession session, String errorType, String reason) {
        try {
            ObjectNode json = JsonNodeFactory.instance.objectNode().put("type", "ERROR").put("errorType", errorType).put("reason", reason);

            session.sendMessage(new TextMessage(json.toString()));
        } catch (IOException e) {
            System.err.println("Error sending error message: " + e.getMessage());
        }
    }

    private String getQueryParam(WebSocketSession session, String name) {
        return Arrays.stream(Objects.requireNonNull(session.getUri()).getQuery().split("&"))
                .map(param -> param.split("="))
                .filter(pair -> pair.length == 2 && pair[0].equals(name))
                .map(pair -> pair[1]).findFirst().orElse("");
    }
}
