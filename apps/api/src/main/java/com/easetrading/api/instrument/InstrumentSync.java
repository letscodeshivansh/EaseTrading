package com.easetrading.api.instrument;

import com.easetrading.api.config.AppProperties;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

/**
 * Loads the stock list into memory ({@link InstrumentStore}).
 *
 *   groww mode : downloads Groww's instrument CSV at startup and every weekday at
 *                8:30 AM IST (all NSE equities, ≈4,300 stocks — a second or two).
 *   mock mode  : 10 well-known NSE stocks.
 *
 * Runs FIRST at startup (@Order(1)) so SubscriptionBootstrap can look stocks up
 * afterwards. If the download fails, it falls back to the 10-stock list so the app
 * still starts (and keeps the previous list on a failed daily refresh).
 */
@Component
@Order(1)
public class InstrumentSync implements ApplicationRunner {

    private final InstrumentStore store;
    private final AppProperties props;

    public InstrumentSync(InstrumentStore store, AppProperties props) {
        this.store = store;
        this.props = props;
    }

    @Override
    public void run(ApplicationArguments args) {
        sync();
    }

    @Scheduled(cron = "0 30 8 * * MON-FRI", zone = "Asia/Kolkata")
    public void daily() {
        if (props.getBroker().isGroww()) sync();
    }

    public synchronized void sync() {
        if (!props.getBroker().isGroww()) {
            store.replaceAll(defaults());
            return;
        }
        try {
            List<Instrument> fresh = download();
            if (fresh.isEmpty()) throw new IllegalStateException("instrument file had no NSE equities");
            store.replaceAll(fresh);
            System.out.printf("[Instruments] Loaded %d NSE equities from Groww.%n", fresh.size());
        } catch (Exception e) {
            System.err.println("[Instruments] Groww instrument download failed: " + e.getMessage());
            if (store.count() == 0) store.replaceAll(defaults());
        }
    }

    private List<Instrument> download() throws Exception {
        HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10))
                .followRedirects(HttpClient.Redirect.NORMAL).build();
        HttpRequest req = HttpRequest.newBuilder(URI.create(props.getGroww().getInstrumentsUrl()))
                .timeout(Duration.ofMinutes(2)).GET().build();
        HttpResponse<java.io.InputStream> res = client.send(req, HttpResponse.BodyHandlers.ofInputStream());
        if (res.statusCode() != 200) throw new IllegalStateException("HTTP " + res.statusCode());
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(res.body(), StandardCharsets.UTF_8))) {
            return InstrumentCsv.parseNseEquities(reader);
        }
    }

    /** A small, real set of NSE stocks (tokens are the NSE exchange tokens). */
    static List<Instrument> defaults() {
        return List.of(
                new Instrument("2885", "RELIANCE", "Reliance Industries Ltd", "NSE", "CASH"),
                new Instrument("11536", "TCS", "Tata Consultancy Services Ltd", "NSE", "CASH"),
                new Instrument("1333", "HDFCBANK", "HDFC Bank Ltd", "NSE", "CASH"),
                new Instrument("4963", "ICICIBANK", "ICICI Bank Ltd", "NSE", "CASH"),
                new Instrument("1594", "INFY", "Infosys Ltd", "NSE", "CASH"),
                new Instrument("3045", "SBIN", "State Bank of India", "NSE", "CASH"),
                new Instrument("10999", "MARUTI", "Maruti Suzuki India Ltd", "NSE", "CASH"),
                new Instrument("1660", "ITC", "ITC Ltd", "NSE", "CASH"),
                new Instrument("3456", "TATAMOTORS", "Tata Motors Ltd", "NSE", "CASH"),
                new Instrument("11483", "LT", "Larsen & Toubro Ltd", "NSE", "CASH"));
    }
}
