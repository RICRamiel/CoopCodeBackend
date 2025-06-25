package org.ricramiel.coopeditbackend.configurations;

import lombok.RequiredArgsConstructor;
import org.ricramiel.coopeditbackend.infrastructure.websocket.handler.CodeWebSocketHandler;
import org.ricramiel.coopeditbackend.infrastructure.websocket.interceptors.AuthHandshakeInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {
    private final AuthHandshakeInterceptor authHandshakeInterceptor;
    private final CodeWebSocketHandler codeWebSocketHandler;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(codeWebSocketHandler, "/ws/code")
                .addInterceptors(authHandshakeInterceptor)
                .setAllowedOrigins("*"); // Для разработки
    }
}