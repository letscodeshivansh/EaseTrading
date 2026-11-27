package com.easetrading.api.fno;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * F&O (index options), read-only.
 *   GET /api/fno/underlyings                         -> NIFTY, BANKNIFTY, FINNIFTY, SENSEX (+ lot sizes)
 *   GET /api/fno/expiries?underlying=NIFTY           -> upcoming expiry dates
 *   GET /api/fno/chain?underlying=NIFTY&expiry=…     -> option chain + PCR, max pain, support/resistance
 */
@RestController
@RequestMapping("/api/fno")
public class FnoController {

    private final FnoService fno;

    public FnoController(FnoService fno) {
        this.fno = fno;
    }

    @GetMapping("/underlyings")
    public List<FnoService.Underlying> underlyings() {
        return fno.underlyings();
    }

    @GetMapping("/expiries")
    public List<LocalDate> expiries(@RequestParam String underlying) {
        return fno.expiries(underlying);
    }

    @GetMapping("/chain")
    public FnoService.ChainView chain(@RequestParam String underlying,
                                      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate expiry) {
        return fno.chain(underlying, expiry);
    }
}
