package com.easetrading.api.broker.groww;

import com.easetrading.api.broker.BrokerModels.*;
import com.easetrading.api.broker.groww.GrowwRateLimiter.Category;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * One user's Groww account data, fetched with THAT user's session
 * (https://groww.in/trade-api/docs/curl — user, margin, portfolio).
 */
@Component
@ConditionalOnProperty(name = "easetrading.broker.provider", havingValue = "groww")
public class GrowwAccountClient {

    private final GrowwHttp http;

    public GrowwAccountClient(GrowwHttp http) {
        this.http = http;
    }

    public GrowwSession newSession(GrowwCredentials creds) {
        return http.newSession(creds);
    }

    public BrokerProfile profile(GrowwSession s) {
        return GrowwMapper.profile(http.get(s, "/v1/user/detail", Map.of(), Category.OTHER));
    }

    public BrokerFunds funds(GrowwSession s) {
        return GrowwMapper.funds(http.get(s, "/v1/margins/detail/user", Map.of(), Category.NON_TRADING));
    }

    public List<BrokerHolding> holdings(GrowwSession s) {
        return GrowwMapper.holdings(http.get(s, "/v1/holdings/user", Map.of(), Category.NON_TRADING));
    }

    public List<BrokerPosition> positions(GrowwSession s) {
        return GrowwMapper.positions(http.get(s, "/v1/positions/user", Map.of("segment", "CASH"), Category.NON_TRADING));
    }
}
