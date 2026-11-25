package com.easetrading.api.dashboard;

import com.easetrading.api.broker.BrokerAdapter;
import com.easetrading.api.broker.BrokerModels.BrokerFunds;
import com.easetrading.api.broker.BrokerModels.BrokerStatus;
import com.easetrading.api.portfolio.BrokerPortfolioService;
import com.easetrading.api.portfolio.BrokerPortfolioService.HoldingView;
import com.easetrading.api.portfolio.BrokerPortfolioService.Summary;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * Everything the home screen needs in ONE call:
 *   GET /api/dashboard
 *
 * Each part is loaded independently — if the broker is down, you still get the mode
 * and status (with the reason), instead of an error page.
 */
@RestController
public class DashboardController {

    private final BrokerAdapter broker;
    private final BrokerPortfolioService portfolio;

    public DashboardController(BrokerAdapter broker, BrokerPortfolioService portfolio) {
        this.broker = broker;
        this.portfolio = portfolio;
    }

    public record Mode(String provider) {}

    public record Dashboard(Mode mode, BrokerStatus broker, BrokerFunds funds, Summary summary,
                            List<HoldingView> topGainers, List<HoldingView> topLosers,
                            List<String> errors) {}

    /** Cheap call for the header badge on every page: which broker (GROWW or MOCK). */
    @GetMapping("/api/dashboard/mode")
    public Mode mode() {
        return new Mode(broker.source());
    }

    @GetMapping("/api/dashboard")
    public Dashboard dashboard() {
        List<String> errors = new java.util.ArrayList<>();
        Mode mode = new Mode(broker.source());
        BrokerStatus status = broker.status();

        BrokerFunds funds = null;
        Summary summary = null;
        List<HoldingView> gainers = List.of(), losers = List.of();

        if (status.connected()) {
            try {
                funds = broker.funds();
            } catch (Exception e) {
                errors.add("Funds: " + e.getMessage());
            }
            try {
                var view = portfolio.build();
                summary = view.summary();
                var withDay = view.holdings().stream().filter(h -> h.dayChangePct() != null).toList();
                gainers = withDay.stream()
                        .filter(h -> h.dayChangePct() > 0)
                        .sorted(Comparator.comparing(HoldingView::dayChangePct, Comparator.reverseOrder()))
                        .limit(3).toList();
                losers = withDay.stream()
                        .filter(h -> h.dayChangePct() < 0)
                        .sorted(Comparator.comparing(HoldingView::dayChangePct))
                        .limit(3).toList();
            } catch (Exception e) {
                errors.add("Portfolio: " + e.getMessage());
            }
        }

        return new Dashboard(mode, status, funds, summary, gainers, losers,
                errors.stream().filter(Objects::nonNull).toList());
    }
}
