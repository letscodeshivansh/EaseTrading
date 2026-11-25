package com.easetrading.api.portfolio;

import com.easetrading.api.broker.BrokerAdapter;
import com.easetrading.api.broker.BrokerModels.BrokerPosition;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * My portfolio.
 *   GET /api/portfolio            -> real broker holdings + live P&L (demo data in mock mode)
 *   GET /api/portfolio/positions  -> today's broker positions
 */
@RestController
@RequestMapping("/api/portfolio")
public class PortfolioController {

    private final BrokerPortfolioService portfolio;
    private final BrokerAdapter broker;

    public PortfolioController(BrokerPortfolioService portfolio, BrokerAdapter broker) {
        this.portfolio = portfolio;
        this.broker = broker;
    }

    @GetMapping
    public BrokerPortfolioService.PortfolioView get() {
        return portfolio.build();
    }

    @GetMapping("/positions")
    public List<BrokerPosition> positions() {
        return broker.positions();
    }
}
