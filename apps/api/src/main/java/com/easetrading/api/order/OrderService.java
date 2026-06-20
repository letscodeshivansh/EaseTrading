package com.easetrading.api.order;

import com.easetrading.api.audit.AuditService;
import com.easetrading.api.common.ApiException;
import com.easetrading.api.common.CurrentUserService;
import com.easetrading.api.instrument.Instrument;
import com.easetrading.api.instrument.InstrumentRepository;
import com.easetrading.api.marketdata.QuoteCache;
import com.easetrading.api.order.OrderEnums.Side;
import com.easetrading.api.order.OrderEnums.Status;
import com.easetrading.api.order.OrderEnums.Type;
import com.easetrading.api.position.Position;
import com.easetrading.api.position.PositionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * The order workflow — the safety-critical heart of Prompt 4.
 *
 *   createDraft(): builds a DRAFT order and runs risk checks. Nothing is sent anywhere.
 *   confirm():     the explicit human step. Only a DRAFT that PASSED risk can be
 *                  confirmed; it is then executed (paper or live) and positions update.
 *
 * No order ever reaches the broker without a human calling confirm(). The AI cannot
 * call confirm().
 */
@Service
public class OrderService {

    private final OrderRepository orders;
    private final RiskEngine riskEngine;
    private final TradeExecutor executor;
    private final PositionRepository positions;
    private final InstrumentRepository instruments;
    private final QuoteCache quoteCache;
    private final AuditService audit;
    private final CurrentUserService currentUser;
    private final ObjectMapper json = new ObjectMapper();

    public OrderService(OrderRepository orders, RiskEngine riskEngine, TradeExecutor executor,
                        PositionRepository positions, InstrumentRepository instruments,
                        QuoteCache quoteCache, AuditService audit, CurrentUserService currentUser) {
        this.orders = orders;
        this.riskEngine = riskEngine;
        this.executor = executor;
        this.positions = positions;
        this.instruments = instruments;
        this.quoteCache = quoteCache;
        this.audit = audit;
        this.currentUser = currentUser;
    }

    /** What the review screen receives: the draft order plus its risk report. */
    public record DraftResult(Order order, RiskReport risk) {}

    @Transactional
    public DraftResult createDraft(String token, Side side, Type type, int qty, double limitPrice) {
        UUID userId = currentUser.currentUserId();
        Instrument inst = instruments.findById(token)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Unknown instrument: " + token));

        double ltp = latestPrice(token);
        double price = (type == Type.LIMIT) ? limitPrice : (ltp > 0 ? ltp : limitPrice);

        Order order = new Order(userId, token, inst.getSymbol(), side, type, qty, price);
        RiskReport risk = riskEngine.check(userId, order, ltp);
        order.setRiskPassed(risk.passed());
        order.setRiskJson(writeJson(risk));
        order.markStatus(Status.DRAFT);
        orders.save(order);

        audit.log(userId, "ORDER_DRAFTED", "ORDER", order.getId().toString(),
                String.format("%s %d %s @ %.2f, riskPassed=%s", side, qty, inst.getSymbol(), price, risk.passed()));

        return new DraftResult(order, risk);
    }

    @Transactional
    public Order confirm(UUID orderId) {
        UUID userId = currentUser.currentUserId();
        Order order = orders.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Order not found"));

        // Idempotency / state guard: only a fresh DRAFT can be confirmed.
        if (order.getStatus() != Status.DRAFT) {
            throw new ApiException(HttpStatus.CONFLICT, "Order already processed (" + order.getStatus() + ")");
        }
        if (!order.isRiskPassed()) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "Order failed risk checks; cannot confirm");
        }

        order.markStatus(Status.PENDING_CONFIRM);

        // Execute (paper simulator or live broker).
        double ltp = latestPrice(order.getToken());
        TradeExecutor.ExecutionResult result = executor.execute(order, ltp);

        order.setBrokerOrderId(result.brokerOrderId());
        order.setFilledPrice(result.fillPrice());
        order.markStatus(result.status());

        if (result.status() == Status.FILLED) {
            applyFill(order);
        }

        orders.save(order);
        audit.log(userId, "ORDER_CONFIRMED", "ORDER", order.getId().toString(),
                "status=" + result.status() + " " + result.message());
        return order;
    }

    @Transactional
    public Order cancel(UUID orderId) {
        UUID userId = currentUser.currentUserId();
        Order order = orders.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Order not found"));
        if (order.getStatus() == Status.FILLED) {
            throw new ApiException(HttpStatus.CONFLICT, "Filled orders cannot be cancelled");
        }
        order.markStatus(Status.CANCELLED);
        orders.save(order);
        audit.log(userId, "ORDER_CANCELLED", "ORDER", order.getId().toString(), "");
        return order;
    }

    public List<Order> list() {
        return orders.findTop50ByUserIdOrderByCreatedAtDesc(currentUser.currentUserId());
    }

    /** Update holdings after a fill. */
    private void applyFill(Order order) {
        Position pos = positions.findByUserIdAndToken(order.getUserId(), order.getToken())
                .orElseGet(() -> new Position(order.getUserId(), order.getToken()));

        if (order.getSide() == Side.BUY) {
            pos.applyBuy(order.getQty(), order.getFilledPrice());
        } else {
            double realized = pos.applySell(order.getQty(), order.getFilledPrice());
            order.setRealizedPnl(realized);
        }
        positions.save(pos);
    }

    /** Read the latest live price from the Redis quote cache, or 0 if unknown. */
    private double latestPrice(String token) {
        String raw = quoteCache.latest(token);
        if (raw == null) return 0;
        try {
            return json.readTree(raw).path("ltp").asDouble(0);
        } catch (Exception e) {
            return 0;
        }
    }

    private String writeJson(Object o) {
        try {
            return json.writeValueAsString(o);
        } catch (Exception e) {
            return "{}";
        }
    }
}
