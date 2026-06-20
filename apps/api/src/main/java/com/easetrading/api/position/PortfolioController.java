package com.easetrading.api.position;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Portfolio holdings, P&L and the risk read-out.
 *   GET /api/portfolio
 */
@RestController
@RequestMapping("/api/portfolio")
public class PortfolioController {

    private final PortfolioService service;

    public PortfolioController(PortfolioService service) {
        this.service = service;
    }

    @GetMapping
    public PortfolioService.PortfolioView get() {
        return service.build();
    }
}
