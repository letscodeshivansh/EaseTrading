package com.easetrading.api.news;

import com.easetrading.api.instrument.Instrument;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class NewsServiceTest {

    private static final String RSS = """
            <?xml version="1.0" encoding="UTF-8"?>
            <rss version="2.0"><channel>
              <item><title>Old story - Moneycontrol.com</title><link>https://news.google.com/a</link>
                <pubDate>Mon, 02 Nov 2026 08:00:00 GMT</pubDate><source url="https://www.moneycontrol.com">Moneycontrol.com</source></item>
              <item><title>Tip of the day - Univest</title><link>https://news.google.com/b</link>
                <pubDate>Thu, 26 Nov 2026 05:52:24 GMT</pubDate><source url="https://univest.in">Univest</source></item>
              <item><title>New story - Reuters</title><link>https://news.google.com/c</link>
                <pubDate>Tue, 24 Nov 2026 10:14:14 GMT</pubDate><source url="https://www.reuters.com">Reuters</source></item>
              <item><title>New story - Reuters</title><link>https://news.google.com/d</link>
                <pubDate>Tue, 24 Nov 2026 09:00:00 GMT</pubDate><source url="https://www.reuters.com">Reuters</source></item>
            </channel></rss>
            """;

    @Test
    void keepsTrustedOutletsNewestFirstWithoutDuplicates() throws Exception {
        List<NewsService.NewsItem> items = NewsService.parse(RSS.getBytes(StandardCharsets.UTF_8));
        assertThat(items).extracting(NewsService.NewsItem::title).containsExactly("New story", "Old story");
        assertThat(items.get(0).source()).isEqualTo("Reuters");
        assertThat(items.get(0).url()).isEqualTo("https://news.google.com/c");
    }

    @Test
    void cleansCompanyNamesForSearch() {
        assertThat(NewsService.searchName(new Instrument("1", "RELIANCE", "RELIANCE INDUSTRIES LTD", "NSE", "CASH")))
                .isEqualTo("Reliance Industries");
        assertThat(NewsService.searchName(new Instrument("2", "TCS", "Tata Consultancy Services Limited", "NSE", "CASH")))
                .isEqualTo("Tata Consultancy Services");
        assertThat(NewsService.searchName(new Instrument("3", "XYZ", "", "NSE", "CASH"))).isEqualTo("XYZ");
    }

    @Test
    void trustedMatchesWholeDomainsOnly() {
        assertThat(NewsService.trusted("https://economictimes.indiatimes.com")).isTrue();
        assertThat(NewsService.trusted("https://www.livemint.com/market")).isTrue();
        assertThat(NewsService.trusted("https://fakemoneycontrol.com")).isFalse();
        assertThat(NewsService.trusted(null)).isFalse();
    }
}
