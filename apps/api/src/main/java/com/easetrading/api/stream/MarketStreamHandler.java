package com.easetrading.api.stream;

import com.easetrading.api.marketdata.MarketSubscription;
import com.easetrading.api.marketdata.QuoteCache;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The live tick WebSocket. The browser connects to /api/stream and receives every
 * price update as JSON. The data path is simply:
 *
 *   feed -> QuoteCache.publish -> (this listener) -> every open browser socket
 *
 * Single api instance, so no message broker is needed in between.
 *
 * Clients send {"action":"subscribe","token":"2885"} to add a stock to the streamed set.
 */
@Component
public class MarketStreamHandler extends TextWebSocketHandler {

    /** Session id -> thread-safe wrapper (sends can come from several threads; slow clients get buffered/dropped). */
    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    private final ObjectMapper json = new ObjectMapper();
    private final MarketSubscription subscription;

    public MarketStreamHandler(QuoteCache quoteCache, MarketSubscription subscription) {
        this.subscription = subscription;
        quoteCache.addListener(this::broadcast);
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        sessions.put(session.getId(), new ConcurrentWebSocketSessionDecorator(session, 5_000, 256 * 1024));
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessions.remove(session.getId());
    }

    /** Handle client control messages, e.g. subscribing to a new symbol. */
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        try {
            JsonNode msg = json.readTree(message.getPayload());
            if ("subscribe".equals(msg.path("action").asText())) {
                subscription.subscribeToken(msg.path("token").asText());
            }
        } catch (Exception ignored) {
            // Malformed control message — safe to ignore.
        }
    }

    /** Called for every published tick; forward it to all browsers. */
    private void broadcast(String tickJson) {
        TextMessage frame = new TextMessage(tickJson);
        sessions.forEach((id, s) -> {
            try {
                if (s.isOpen()) s.sendMessage(frame);
                else sessions.remove(id);
            } catch (Exception e) {
                sessions.remove(id);
            }
        });
    }
}
