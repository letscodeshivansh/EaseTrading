package com.easetrading.api.instrument;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InstrumentRepository extends JpaRepository<Instrument, String> {

    /** Case-insensitive "starts with" search on symbol or name, for the search box. */
    List<Instrument> findTop20BySymbolStartingWithIgnoreCaseOrNameContainingIgnoreCase(
            String symbol, String name);

    Optional<Instrument> findFirstBySymbolIgnoreCaseAndExchange(String symbol, String exchange);
}
