package com.easetrading.api.order;

import com.easetrading.api.order.OrderEnums.Side;
import com.easetrading.api.order.OrderEnums.Type;
import com.easetrading.api.order.OrderService.DraftResult;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Order endpoints — the two-step, human-confirmed flow.
 *
 *   POST /api/orders             -> create a DRAFT and return the risk report
 *   POST /api/orders/{id}/confirm-> the explicit human confirmation -> execute
 *   POST /api/orders/{id}/cancel -> cancel a non-filled order
 *   GET  /api/orders             -> recent order history
 *
 * There is intentionally NO endpoint that places an order in one shot — the draft +
 * confirm split is the core safety control.
 */
@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    public record CreateOrderRequest(
            @NotNull String token,
            @NotNull Side side,
            @NotNull Type type,
            @Positive int qty,
            double price) {}   // price required for LIMIT orders

    @PostMapping
    public DraftResult create(@RequestBody CreateOrderRequest req) {
        return orderService.createDraft(req.token(), req.side(), req.type(), req.qty(), req.price());
    }

    @PostMapping("/{id}/confirm")
    public Order confirm(@PathVariable UUID id) {
        return orderService.confirm(id);
    }

    @PostMapping("/{id}/cancel")
    public Order cancel(@PathVariable UUID id) {
        return orderService.cancel(id);
    }

    @GetMapping
    public List<Order> list() {
        return orderService.list();
    }
}
