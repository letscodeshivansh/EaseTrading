package com.easetrading.api.position;

import com.easetrading.api.broker.BrokerAdapter;
import com.easetrading.api.broker.BrokerModels.BrokerPosition;
import com.easetrading.api.portfolio.BrokerPortfolioService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Portfolio endpoints.
 *   GET /api/portfolio            -> MY PORTFOLIO: real broker holdings + live P&L (demo data in mock mode)
 *   GET /api/portfolio/positions  -> today's broker positions
 *   GET /api/portfolio/paper      -> holdings built from paper trades placed in this app
 */
@RestController
@RequestMapping("/api/portfolio")
public class PortfolioController {

    private final BrokerPortfolioService brokerPortfolio;
    private final PortfolioService paperPortfolio;
    private final BrokerAdapter broker;

    public PortfolioController(BrokerPortfolioService brokerPortfolio, PortfolioService paperPortfolio,
                               BrokerAdapter broker) {
        this.brokerPortfolio = brokerPortfolio;
        this.paperPortfolio = paperPortfolio;
        this.broker = broker;
    }

    @GetMapping
    public BrokerPortfolioService.PortfolioView get() {
        return brokerPortfolio.build();
    }

    @GetMapping("/positions")
    public List<BrokerPosition> positions() {
        return broker.positions();
    }

    @GetMapping("/paper")
    public PortfolioService.PortfolioView paper() {
        return paperPortfolio.build();
    }
}
