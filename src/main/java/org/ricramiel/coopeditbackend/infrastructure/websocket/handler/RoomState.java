package org.ricramiel.coopeditbackend.infrastructure.websocket.handler;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
public class RoomState {
    // Остальные методы без изменений
    @Getter
    private String content = "";
    @Getter
    private int version = 0;
    private final List<Operation> operationHistory = new ArrayList<>();
    private final Map<String, SessionInfo> sessions = new ConcurrentHashMap<>();

    public synchronized void processOperations(List<Operation> clientOperations,
                                               int clientVersion,
                                               String userId,
                                               String sessionId) {

        try {
            // 1. Получаем пропущенные операции
            List<Operation> missedOps = new ArrayList<>(getMissedOperations(clientVersion));

            // 2. Трансформируем и применяем каждую операцию
            List<Operation> transformedOperations = new ArrayList<>();
            List<Integer> appliedOperationIds = new ArrayList<>();

            for (Operation clientOp : clientOperations) {
                // Трансформируем против всех пропущенных операций
                Operation transformed = clientOp;
                for (Operation missedOp : missedOps) {
                    if (missedOp.getUserId().equals(clientOp.getUserId())) {
                        continue;
                    }
                    transformed = OperationTransformer.transform(transformed, missedOp);
                }

                // Применяем к документу
                content = OperationApplier.apply(content, transformed);
                version++;
                transformed.setServerVersion(version);
                operationHistory.add(transformed);
                transformedOperations.add(transformed);
                appliedOperationIds.add(clientOp.getId());
            }

            // 3. Рассылаем трансформированные операции
            if (!transformedOperations.isEmpty()) {
                broadcastOperations(transformedOperations, userId, sessionId);
            }

            // 4. Отправляем подтверждение автору изменений
            sendConfirmation(userId, appliedOperationIds);
        }
        catch (Exception e) {
            sendErrorConfirmation(userId, clientOperations.stream().map(Operation::getId).collect(Collectors.toList()));
            log.error("Error processing operations in room state", e);
        }
    }

    public void processCursorUpdate(String userId, String sessionId, String roomId, int position, String color) {
        broadcastCursorPosition(userId, roomId, position, color, sessionId);
    }

    private void broadcastCursorPosition(String userId, String roomId, int position, String color, String excludeSessionId) {
        ObjectNode json = JsonNodeFactory.instance.objectNode()
                .put("type", "REMOTE_CURSOR")
                .put("userId", userId)
                .put("roomId", roomId)
                .put("position", position)
                .put("color", color);

        sessions.values().stream()
                .filter(sessionInfo -> !sessionInfo.session.getId().equals(excludeSessionId))
                .forEach(sessionInfo -> {
                    try {
                        sessionInfo.session.sendMessage(new TextMessage(json.toString()));
                    } catch (IOException e) {
                        log.error("Error broadcasting cursor info: {}", e.getMessage());
                    }
                });
    }

    private void sendConfirmation(String userId, List<Integer> operationIds) {
        SessionInfo authorSession = sessions.values().stream()
                .filter(si -> si.userId.equals(userId))
                .findFirst()
                .orElse(null);

        if (authorSession != null) {
            try {
                ObjectNode json = JsonNodeFactory.instance.objectNode()
                        .put("type", "OPERATIONS_CONFIRMED")
                        .putPOJO("appliedOperationIds", operationIds)
                        .put("newVersion", version);

                authorSession.session.sendMessage(new TextMessage(json.toString()));
            } catch (IOException e) {
                log.error("Error sending confirmation: {}", e.getMessage());
            }
        }
    }

    private void sendErrorConfirmation(String userId, List<Integer> operationIds) {
        SessionInfo authorSession = sessions.values().stream()
                .filter(si -> si.userId.equals(userId))
                .findFirst()
                .orElse(null);

        if (authorSession != null) {
            try {
                ObjectNode json = JsonNodeFactory.instance.objectNode()
                        .put("type", "OPERATIONS_DECLINED")
                        .putPOJO("declinedOperationIds", operationIds)
                        .put("newVersion", version);

                authorSession.session.sendMessage(new TextMessage(json.toString()));
            } catch (IOException e) {
                log.error("Error sending error confirmation: {}", e.getMessage());
            }
        }
    }

    private List<Operation> getMissedOperations(int clientVersion) {
        if (clientVersion < 0 || clientVersion >= version) {
            return Collections.emptyList();
        }
        return operationHistory.subList(clientVersion, operationHistory.size());
    }

    private void broadcastOperations(List<Operation> operations,
                                     String authorId,
                                     String excludeSessionId) {
        ObjectNode json = JsonNodeFactory.instance.objectNode()
                .put("type", "REMOTE_OPERATIONS")
                .putPOJO("operations", operations)
                .put("userId", authorId)
                .put("newVersion", version);

        sessions.values().stream()
                .filter(sessionInfo -> !sessionInfo.session.getId().equals(excludeSessionId))
                .forEach(sessionInfo -> {
                    try {
                        sessionInfo.session.sendMessage(new TextMessage(json.toString()));
                    } catch (IOException e) {
                        log.error("Error broadcasting operations: {}", e.getMessage());
                    }
                });
    }

    public synchronized void broadcastCodeSnapshot() {
        ObjectNode json = JsonNodeFactory.instance.objectNode()
                .put("type", "CODE_SNAPSHOT")
                .putPOJO("content", content)
                .put("newVersion", version);

        sessions.values()
                .forEach(sessionInfo -> {
                    try {
                        sessionInfo.session.sendMessage(new TextMessage(json.toString()));
                    } catch (IOException e) {
                        log.error("Error broadcasting snapshot: {}", e.getMessage());
                    }
                });
    }

    public boolean isEmpty() { return sessions.isEmpty(); }

    public void addSession(WebSocketSession session, String userId) {
        sessions.put(session.getId(), new SessionInfo(session, userId));
    }

    public void removeSession(String sessionId) {
        sessions.remove(sessionId);
    }

    private static class SessionInfo {
        WebSocketSession session;
        String userId;

        SessionInfo(WebSocketSession session, String userId) {
            this.session = session;
            this.userId = userId;
        }
    }
}