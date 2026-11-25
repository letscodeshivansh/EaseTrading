package com.easetrading.api.portfolio;

import com.easetrading.api.account.AccountService;
import com.easetrading.api.broker.BrokerModels.BrokerPosition;
import com.easetrading.api.common.CurrentUserService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * My portfolio.
 *   GET /api/portfolio            -> MY broker holdings + live P&L (demo data in mock mode)
 *   GET /api/portfolio/positions  -> my positions today
 */
@RestController
@RequestMapping("/api/portfolio")
public class PortfolioController {

    private final BrokerPortfolioService portfolio;
    private final AccountService accounts;
    private final CurrentUserService currentUser;

    public PortfolioController(BrokerPortfolioService portfolio, AccountService accounts, CurrentUserService currentUser) {
        this.portfolio = portfolio;
        this.accounts = accounts;
        this.currentUser = currentUser;
    }

    @GetMapping
    public BrokerPortfolioService.PortfolioView get() {
        return portfolio.build();
    }

    @GetMapping("/positions")
    public List<BrokerPosition> positions() {
        return accounts.positions(currentUser.currentUserId());
    }
}
