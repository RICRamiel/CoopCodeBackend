package org.ricramiel.coopeditbackend.infrastructure.websocket.interceptors;

import jakarta.servlet.http.HttpServletRequest;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ricramiel.coopeditbackend.domain.models.enums.Role;
import org.ricramiel.coopeditbackend.domain.models.enums.RoomAction;
import org.ricramiel.coopeditbackend.infrastructure.repositories.RoomRepository;
import org.ricramiel.coopeditbackend.infrastructure.services.JwtAccessTokenUtil;
import org.ricramiel.coopeditbackend.infrastructure.services.RoomAccessManager;
import org.ricramiel.coopeditbackend.infrastructure.websocket.common.CustomWebSocketAttributeKeys;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuthHandshakeInterceptor implements HandshakeInterceptor {
    private final RoomAccessManager roomAccessManager;
    private static final String BEARER_PREFIX = "Bearer ";
    private final JwtAccessTokenUtil accessTokenService;

    @Override
    public boolean beforeHandshake(@NonNull ServerHttpRequest request, @NonNull ServerHttpResponse response, @NonNull WebSocketHandler wsHandler, @NonNull Map<String, Object> attributes) {
        ServletServerHttpRequest servletRequest = (ServletServerHttpRequest) request;
        HttpServletRequest httpRequest = servletRequest.getServletRequest();

        UUID roomId;

        try {
            roomId = UUID.fromString(httpRequest.getParameter("room"));
        } catch (Exception e) {
            response.setStatusCode(HttpStatus.NOT_FOUND);
            return false;
        }

        if (!roomAccessManager.hasAccessTo(roomId, RoomAction.JOIN)) {
            response.setStatusCode(HttpStatus.FORBIDDEN);
            return false;
        }

        String tokenValue = getTokenFromRequestOrNull(request);
        log.info(tokenValue);
        if (tokenValue == null || tokenValue.isEmpty()) {
            log.info("Token is empty");
            String anonId = "anon_" + UUID.randomUUID();
            String anonUserName = anonId.substring(0, 13);

            attributes.put(CustomWebSocketAttributeKeys.USER_ID, anonId);
            attributes.put(CustomWebSocketAttributeKeys.USER_NAME, anonUserName);
            return true;
        }

        UUID id = accessTokenService.extractId(tokenValue);
        Set<Role> roles = accessTokenService.extractRoles(tokenValue);
        String name = accessTokenService.extractName(tokenValue);
        attributes.put(CustomWebSocketAttributeKeys.ROLES, roles);
        attributes.put(CustomWebSocketAttributeKeys.USER_NAME, name);
        attributes.put(CustomWebSocketAttributeKeys.TOKEN, tokenValue);
        attributes.put(CustomWebSocketAttributeKeys.USER_ID, id.toString());

        return true;
    }

    @Override
    public void afterHandshake(@NonNull ServerHttpRequest request, @NonNull ServerHttpResponse response, @NonNull WebSocketHandler wsHandler, Exception exception) {
    }

    private String getTokenFromRequestOrNull(ServerHttpRequest request) {
        final String authorizationHeader = request.getHeaders().getFirst(HttpHeaders.COOKIE);
        if (StringUtils.hasText(authorizationHeader) && authorizationHeader.startsWith("AUTH_TOKEN=")) {
            String temp = authorizationHeader.substring("AUTH_TOKEN=".length());
            return accessTokenService.isTokenValid(temp) ? temp : null;
        }
        return null;
    }
}