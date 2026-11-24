package com.easetrading.api.watchlist;

import com.easetrading.api.watchlist.WatchlistService.WatchRow;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * My watchlist.
 *   GET    /api/watchlist          -> my stocks with latest prices
 *   POST   /api/watchlist {token}  -> add a stock
 *   DELETE /api/watchlist/{token}  -> remove a stock
 */
@RestController
@RequestMapping("/api/watchlist")
public class WatchlistController {

    private final WatchlistService service;

    public WatchlistController(WatchlistService service) {
        this.service = service;
    }

    public record AddRequest(@NotBlank String token) {}

    @GetMapping
    public List<WatchRow> list() {
        return service.list();
    }

    @PostMapping
    public List<WatchRow> add(@RequestBody AddRequest req) {
        return service.add(req.token());
    }

    @DeleteMapping("/{token}")
    public List<WatchRow> remove(@PathVariable String token) {
        return service.remove(token);
    }
}
