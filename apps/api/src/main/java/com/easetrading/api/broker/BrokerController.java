package com.easetrading.api.broker;

import com.easetrading.api.broker.BrokerModels.BrokerFunds;
import com.easetrading.api.broker.BrokerModels.BrokerProfile;
import com.easetrading.api.broker.BrokerModels.BrokerStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Broker account info (read-only).
 *   GET  /api/broker/status    -> connected? token expiry, what to do if not connected
 *   POST /api/broker/reconnect -> log in again now (after approving the key on Groww)
 *   GET /api/broker/profile  -> client code, enabled exchanges/segments
 *   GET /api/broker/funds    -> available cash and margin
 *
 * Credentials are never accepted or returned over HTTP — they live only in .env.
 */
@RestController
@RequestMapping("/api/broker")
public class BrokerController {

    private final BrokerAdapter broker;

    public BrokerController(BrokerAdapter broker) {
        this.broker = broker;
    }

    @GetMapping("/status")
    public BrokerStatus status() {
        return broker.status();
    }

    @PostMapping("/reconnect")
    public BrokerStatus reconnect() {
        try {
            broker.reconnect();
        } catch (Exception ignored) {
            // The status below carries the reason.
        }
        return broker.status();
    }

    @GetMapping("/profile")
    public BrokerProfile profile() {
        return broker.profile();
    }

    @GetMapping("/funds")
    public BrokerFunds funds() {
        return broker.funds();
    }
}
