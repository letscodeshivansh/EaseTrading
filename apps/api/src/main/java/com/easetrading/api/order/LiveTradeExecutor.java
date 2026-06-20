package com.easetrading.api.order;

import com.easetrading.api.broker.AngelOneAdapter;
import com.easetrading.api.broker.BrokerSessionProvider;
import com.easetrading.api.instrument.Instrument;
import com.easetrading.api.instrument.InstrumentRepository;
import com.easetrading.api.order.OrderEnums.Status;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Real execution via Angel One. Active only when easetrading.trading.mode = live.
 *
 * Sends the order to the broker and returns SUBMITTED with the broker order id. The
 * actual fill arrives asynchronously; a follow-up order-status poll (a future
 * enhancement) would move it to FILLED. Until then, paper mode gives the full
 * fill flow for testing.
 */
@Component
@ConditionalOnProperty(name = "easetrading.trading.mode", havingValue = "live")
public class LiveTradeExecutor implements TradeExecutor {

    private final AngelOneAdapter angelOne;
    private final BrokerSessionProvider sessionProvider;
    private final InstrumentRepository instruments;

    public LiveTradeExecutor(AngelOneAdapter angelOne, BrokerSessionProvider sessionProvider,
                             InstrumentRepository instruments) {
        this.angelOne = angelOne;
        this.sessionProvider = sessionProvider;
        this.instruments = instruments;
        System.out.println("[Trading] LIVE mode — orders go to Angel One with real money.");
    }

    @Override
    public ExecutionResult execute(Order order, double ltp) {
        Instrument inst = instruments.findById(order.getToken()).orElseThrow();
        try {
            String brokerId = angelOne.placeOrder(
                    sessionProvider.get(), inst.getExchange(), inst.getSymbol(), inst.getToken(),
                    order.getSide().name(), order.getType().name(), order.getQty(), order.getPrice());
            return new ExecutionResult(brokerId, Status.SUBMITTED, 0, "Submitted to Angel One");
        } catch (Exception e) {
            return new ExecutionResult(null, Status.REJECTED, 0, "Broker rejected: " + e.getMessage());
        }
    }
}
