package com.easetrading.api.news;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** GET /api/news/{token} -> latest headlines about that stock from major business outlets. */
@RestController
@RequestMapping("/api/news")
public class NewsController {

    private final NewsService news;

    public NewsController(NewsService news) {
        this.news = news;
    }

    @GetMapping("/{token}")
    public NewsService.NewsView forStock(@PathVariable String token) {
        return news.forStock(token);
    }
}
