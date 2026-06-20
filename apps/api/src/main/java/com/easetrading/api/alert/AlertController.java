package com.easetrading.api.alert;

import jakarta.validation.constraints.NotNull;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Alert management.
 *   GET    /api/alerts          -> list my alerts
 *   POST   /api/alerts          -> create an alert
 *   DELETE /api/alerts/{id}     -> remove an alert
 */
@RestController
@RequestMapping("/api/alerts")
public class AlertController {

    private final AlertService service;

    public AlertController(AlertService service) {
        this.service = service;
    }

    public record CreateAlertRequest(
            @NotNull String token,
            String symbol,
            @NotNull Alert.Type type,
            @NotNull Alert.Operator operator,
            double threshold) {}

    @GetMapping
    public List<Alert> list() {
        return service.list();
    }

    @PostMapping
    public Alert create(@RequestBody CreateAlertRequest req) {
        return service.create(req.token(), req.symbol(), req.type(), req.operator(), req.threshold());
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable UUID id) {
        service.delete(id);
    }
}
