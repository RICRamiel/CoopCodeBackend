package org.ricramiel.coopeditbackend.infrastructure.websocket.interceptors;

import jakarta.servlet.http.HttpServletRequest;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.ricramiel.coopeditbackend.infrastructure.services.JwtAccessTokenUtil;
import org.ricramiel.coopeditbackend.infrastructure.websocket.common.CustomWebSocketAttributeKeys;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class AuthHandshakeInterceptor implements HandshakeInterceptor {
    private static final String BEARER_PREFIX = "Bearer ";
    private final JwtAccessTokenUtil accessTokenService;

    @Override
    public boolean beforeHandshake(@NonNull ServerHttpRequest request, @NonNull ServerHttpResponse response,
                                   @NonNull WebSocketHandler wsHandler, @NonNull Map<String, Object> attributes) {

        String tokenValue = getTokenFromRequestOrNull(request);

        if (!StringUtils.hasText(tokenValue)) {
            attributes.put(CustomWebSocketAttributeKeys.IS_USER_ANONYMOUS, true);
            attributes.put(CustomWebSocketAttributeKeys.USER_ID, "anon_" + UUID.randomUUID());
            return true;
        }

        UUID id = accessTokenService.extractId(tokenValue);
        attributes.put(CustomWebSocketAttributeKeys.IS_USER_ANONYMOUS, false);
        attributes.put(CustomWebSocketAttributeKeys.TOKEN, tokenValue);
        attributes.put(CustomWebSocketAttributeKeys.USER_ID, id.toString());

        return true;
    }

    @Override
    public void afterHandshake(@NonNull ServerHttpRequest request, @NonNull ServerHttpResponse response,
                               @NonNull WebSocketHandler wsHandler, Exception exception) {
    }

    private String getTokenFromRequestOrNull(ServerHttpRequest request) {
        final String authorizationHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (StringUtils.hasText(authorizationHeader) && authorizationHeader.startsWith(BEARER_PREFIX)) {
            return authorizationHeader.substring(BEARER_PREFIX.length());
        }
        return null;
    }
}