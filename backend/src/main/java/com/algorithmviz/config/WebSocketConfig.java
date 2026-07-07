package com.algorithmviz.config;

import com.algorithmviz.websocket.SignalingHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {
    private final SignalingHandler signalingHandler;
    private final String allowedOrigin;

    public WebSocketConfig(
            SignalingHandler signalingHandler,
            @Value("${app.cors.allowed-origin:http://localhost:4200}") String allowedOrigin
    ) {
        this.signalingHandler = signalingHandler;
        this.allowedOrigin = allowedOrigin;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(signalingHandler, "/ws/signaling")
                .setAllowedOrigins(allowedOrigin);
    }
}
