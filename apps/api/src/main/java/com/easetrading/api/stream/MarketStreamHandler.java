package com.easetrading.api.stream;

import com.easetrading.api.marketdata.MarketSubscription;
import com.easetrading.api.marketdata.QuoteCache;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The live tick WebSocket. The browser connects to /api/stream and receives every
 * price update as JSON. The data path is:
 *
 *   feed -> QuoteCache.publish -> Redis "chan:ticks" -> (this listener) -> all sockets
 *
 * Using Redis as the middle layer means multiple api instances all forward ticks,
 * so this scales horizontally without code changes.
 *
 * Clients can optionally send {"action":"subscribe","token":"2885"} to add a symbol
 * to the streamed set; otherwise the default seeded watchlist streams.
 */
@Component
public class MarketStreamHandler extends TextWebSocketHandler implements MessageListener {

    // All currently-connected browser sessions.
    private final Set<WebSocketSession> sessions = ConcurrentHashMap.newKeySet();
    private final ObjectMapper json = new ObjectMapper();
    private final MarketSubscription subscription;

    public MarketStreamHandler(RedisMessageListenerContainer listenerContainer,
                               MarketSubscription subscription) {
        this.subscription = subscription;
        // Subscribe to the Redis ticks channel; onMessage() fires for every tick.
        listenerContainer.addMessageListener(this, new ChannelTopic(QuoteCache.TICKS_CHANNEL));
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        sessions.add(session);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessions.remove(session);
    }

    /** Handle client control messages, e.g. subscribing to a new symbol. */
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        try {
            JsonNode msg = json.readTree(message.getPayload());
            if ("subscribe".equals(msg.path("action").asText())) {
                subscription.subscribeToken(msg.path("token").asText());
            } else if ("unsubscribe".equals(msg.path("action").asText())) {
                subscription.remove(msg.path("token").asText());
            }
        } catch (Exception ignored) {
            // Malformed control message — safe to ignore.
        }
    }

    /** Called by Redis for every published tick; forward it to all browsers. */
    @Override
    public void onMessage(Message redisMessage, byte[] pattern) {
        String payload = new String(redisMessage.getBody());
        TextMessage frame = new TextMessage(payload);
        for (WebSocketSession s : sessions) {
            try {
                if (s.isOpen()) s.sendMessage(frame);
            } catch (Exception e) {
                sessions.remove(s);
            }
        }
    }
}
