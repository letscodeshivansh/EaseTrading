package com.easetrading.api.instrument;

import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.ApplicationArguments;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Seeds a small set of popular NSE instruments on startup so search, charts and the
 * mock feed work out of the box.
 *
 * TODO (Prompt 1 -> production): replace this seed with a daily scheduled job that
 * downloads Angel One's full scrip-master JSON and upserts every NSE/BSE instrument.
 * The seam is here; nothing else needs to change.
 *
 * Tokens below are the real Angel One NSE tokens for these names.
 */
@Component
public class SymbolMasterSeeder implements ApplicationRunner {

    private final InstrumentRepository repo;

    public SymbolMasterSeeder(InstrumentRepository repo) {
        this.repo = repo;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (repo.count() > 0) return; // already seeded

        List<Instrument> seed = List.of(
                new Instrument("2885", "RELIANCE", "Reliance Industries Ltd", "NSE", "EQ"),
                new Instrument("11536", "TCS", "Tata Consultancy Services Ltd", "NSE", "EQ"),
                new Instrument("1333", "HDFCBANK", "HDFC Bank Ltd", "NSE", "EQ"),
                new Instrument("4963", "ICICIBANK", "ICICI Bank Ltd", "NSE", "EQ"),
                new Instrument("1594", "INFY", "Infosys Ltd", "NSE", "EQ"),
                new Instrument("3045", "SBIN", "State Bank of India", "NSE", "EQ"),
                new Instrument("10999", "MARUTI", "Maruti Suzuki India Ltd", "NSE", "EQ"),
                new Instrument("1660", "ITC", "ITC Ltd", "NSE", "EQ"),
                new Instrument("3456", "TATAMOTORS", "Tata Motors Ltd", "NSE", "EQ"),
                new Instrument("11483", "LT", "Larsen & Toubro Ltd", "NSE", "EQ")
        );
        repo.saveAll(seed);
    }
}
