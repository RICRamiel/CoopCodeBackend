package org.ricramiel.coopeditbackend.infrastructure.websocket.handler;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.ricramiel.coopeditbackend.domain.models.enums.Role;
import org.ricramiel.coopeditbackend.domain.models.enums.RoomAccessMode;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
public class RoomState {
    @Getter @Setter
    private RoomAccessMode accessMode;
    @Getter @Setter
    private UUID ownerId;
    @Getter @Setter
    private UUID id;
    @Getter @Setter
    private String name;
    @Getter @Setter
    private String content;

    public RoomState(RoomAccessMode accessMode, UUID ownerId, UUID id, String name, String content) {
        this.accessMode = accessMode;
        this.ownerId = ownerId;
        this.id = id;
        this.name = name;
        this.content = content;
    }

    @Getter
    private int version = 0;

    private final List<Operation> operationHistory = new ArrayList<>();
    @Getter
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

    public void processCursorUpdate(String sessionId, int position, String color) {
        SessionInfo sessionInfo = sessions.get(sessionId);
        broadcastCursorPosition(sessionInfo.userName, position, color, sessionId);
    }

    private void broadcastCursorPosition(String userId, int position, String color, String excludeSessionId) {
        ObjectNode json = JsonNodeFactory.instance.objectNode()
                .put("type", "REMOTE_CURSOR")
                .put("userId", userId)
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

    public void broadcastCodeSnapshot() {
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

    public void addSession(SessionInfo sessionInfo) {
        sessions.put(sessionInfo.session.getId(), sessionInfo);
    }

    public void removeSession(String sessionId) {
        sessions.remove(sessionId);
    }

    public static class SessionInfo {
        public WebSocketSession session;
        public String userId;
        public String userName;
        //public Set<Role> userRoles;

        SessionInfo(WebSocketSession session, String userId, Set<Role> userRoles, String userName) {
            this.session = session;
            this.userId = userId;
            this.userName = userName;
            //this.userRoles = userRoles;
        }
    }
}