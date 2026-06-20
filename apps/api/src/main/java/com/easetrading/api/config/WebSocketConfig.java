package com.easetrading.api.config;

import com.easetrading.api.stream.MarketStreamHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * Registers the live market WebSocket at ws://<host>/api/stream.
 * setAllowedOrigins("*") is fine for local dev; lock this down to the real frontend
 * origin in production.
 */
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final MarketStreamHandler handler;

    public WebSocketConfig(MarketStreamHandler handler) {
        this.handler = handler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(handler, "/api/stream").setAllowedOrigins("*");
    }
}
