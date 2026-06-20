package com.easetrading.api.fundamental;

import org.springframework.web.bind.annotation.*;

/**
 * Fundamentals for one instrument.
 *   GET /api/fundamentals/{token}
 */
@RestController
@RequestMapping("/api/fundamentals")
public class FundamentalController {

    private final FundamentalService service;

    public FundamentalController(FundamentalService service) {
        this.service = service;
    }

    @GetMapping("/{token}")
    public Fundamental get(@PathVariable String token) {
        return service.getForToken(token);
    }
}
