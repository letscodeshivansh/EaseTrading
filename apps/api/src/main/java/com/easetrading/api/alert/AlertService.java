package com.easetrading.api.alert;

import com.easetrading.api.analysis.AnalysisClient;
import com.easetrading.api.audit.AuditService;
import com.easetrading.api.common.CurrentUserService;
import com.easetrading.api.marketdata.CandleDto;
import com.easetrading.api.marketdata.CandleService;
import com.easetrading.api.marketdata.QuoteCache;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

/**
 * Evaluates alerts on a schedule and fires the ones whose condition is met.
 *
 *  - PRICE alerts read the latest live price from the quote cache (cheap).
 *  - RSI alerts compute the indicator from candles (a little heavier, so we keep
 *    the evaluation interval modest).
 *
 * When an alert fires it is marked TRIGGERED, audited, and the user is notified.
 */
@Service
public class AlertService {

    private final AlertRepository alerts;
    private final QuoteCache quoteCache;
    private final CandleService candleService;
    private final AnalysisClient analysis;
    private final AuditService audit;
    private final CurrentUserService currentUser;
    private final NotificationService notifications;
    private final ObjectMapper json = new ObjectMapper();

    public AlertService(AlertRepository alerts, QuoteCache quoteCache, CandleService candleService,
                        AnalysisClient analysis, AuditService audit, CurrentUserService currentUser,
                        NotificationService notifications) {
        this.alerts = alerts;
        this.quoteCache = quoteCache;
        this.candleService = candleService;
        this.analysis = analysis;
        this.audit = audit;
        this.currentUser = currentUser;
        this.notifications = notifications;
    }

    // ---- CRUD (scoped to the current user) ----

    public Alert create(String token, String symbol, Alert.Type type, Alert.Operator op, double threshold) {
        Alert a = new Alert(currentUser.currentUserId(), token, symbol, type, op, threshold);
        return alerts.save(a);
    }

    public List<Alert> list() {
        return alerts.findByUserIdOrderByCreatedAtDesc(currentUser.currentUserId());
    }

    public void delete(UUID id) {
        alerts.deleteById(id);
    }

    // ---- Evaluation loop ----

    /** Runs every 10 seconds; checks all ACTIVE alerts. */
    @Scheduled(fixedRate = 10_000)
    public void evaluateAll() {
        for (Alert alert : alerts.findByStatus(Alert.Status.ACTIVE)) {
            try {
                Double value = currentValue(alert);
                if (value == null) continue;
                alert.setLastValue(value);

                if (alert.isTriggeredBy(value)) {
                    alert.trigger(value);
                    audit.log(alert.getUserId(), "ALERT_TRIGGERED", "ALERT", alert.getId().toString(),
                            String.format("%s %s %s %.2f (value %.2f)", alert.getSymbol(),
                                    alert.getType(), alert.getOperator(), alert.getThreshold(), value));
                    notifications.notifyUser(alert.getUserId(),
                            String.format("%s %s alert: value %.2f %s %.2f",
                                    alert.getSymbol(), alert.getType(), value,
                                    alert.getOperator() == Alert.Operator.GT ? ">" : "<", alert.getThreshold()));
                }
                alerts.save(alert);
            } catch (Exception e) {
                System.err.println("Alert eval failed for " + alert.getId() + ": " + e.getMessage());
            }
        }
    }

    /** Get the metric value this alert watches. */
    private Double currentValue(Alert alert) {
        if (alert.getType() == Alert.Type.PRICE) {
            String raw = quoteCache.latest(alert.getToken());
            if (raw == null) return null;
            double ltp = readLtp(raw);
            return ltp > 0 ? ltp : null;
        }
        if (alert.getType() == Alert.Type.RSI) {
            Instant to = Instant.now();
            Instant from = to.minus(250, ChronoUnit.DAYS);
            List<CandleDto> candles = candleService.getCandles(alert.getToken(), "1d", from, to);
            JsonNode ind = analysis.indicators(alert.getToken(), "1d", candles);
            JsonNode rsi = ind.path("rsi14");
            return rsi.isNumber() ? rsi.asDouble() : null;
        }
        return null;
    }

    private double readLtp(String quoteJson) {
        try {
            return json.readTree(quoteJson).path("ltp").asDouble(0);
        } catch (Exception e) {
            return 0;
        }
    }
}
