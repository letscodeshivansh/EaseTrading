package com.easetrading.api.news;

import com.easetrading.api.common.ApiException;
import com.easetrading.api.common.TtlCache;
import com.easetrading.api.instrument.Instrument;
import com.easetrading.api.instrument.InstrumentStore;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Latest news for one stock, from major Indian / global business outlets.
 *
 * Source: the public Google News RSS search feed (it already collects articles
 * from Economic Times, Moneycontrol, Mint, Business Standard, Reuters, …). We keep
 * only the outlets in {@link #TRUSTED}, newest first, and cache each stock for 15 min.
 * Only the company name is sent to Google — never anything about the user.
 */
@Service
public class NewsService {

    public record NewsItem(String title, String source, String url, Instant publishedAt) {}
    public record NewsView(String query, List<NewsItem> items) {}

    /** Outlet websites we show. Matched against the feed's <source url="…">. */
    static final Set<String> TRUSTED = Set.of(
            "economictimes.indiatimes.com", "moneycontrol.com", "livemint.com", "business-standard.com",
            "reuters.com", "bloomberg.com", "cnbctv18.com", "ndtvprofit.com", "financialexpress.com",
            "thehindubusinessline.com", "businesstoday.in", "zeebiz.com", "thehindu.com");

    private static final int MAX_ITEMS = 12;
    private static final String FEED = "https://news.google.com/rss/search?hl=en-IN&gl=IN&ceid=IN:en&q=";

    private final InstrumentStore store;
    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10)).followRedirects(HttpClient.Redirect.NORMAL).build();
    private final Map<String, TtlCache<NewsView>> cache = new ConcurrentHashMap<>();

    public NewsService(InstrumentStore store) {
        this.store = store;
    }

    public NewsView forStock(String token) {
        Instrument inst = store.get(token)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Unknown stock: " + token));
        return cache.computeIfAbsent(token, k -> new TtlCache<>(Duration.ofMinutes(15)))
                .get(() -> fetch(searchName(inst)));
    }

    private NewsView fetch(String name) {
        String query = "\"" + name + "\" when:30d";
        HttpRequest req = HttpRequest.newBuilder(URI.create(FEED + URLEncoder.encode(query, StandardCharsets.UTF_8)))
                .timeout(Duration.ofSeconds(10)).header("User-Agent", "Mozilla/5.0 (EaseTrading news reader)").GET().build();
        try {
            HttpResponse<byte[]> res = http.send(req, HttpResponse.BodyHandlers.ofByteArray());
            if (res.statusCode() != 200) throw new IllegalStateException("HTTP " + res.statusCode());
            return new NewsView(name, parse(res.body()));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ApiException(HttpStatus.BAD_GATEWAY, "Couldn't load news right now");
        } catch (Exception e) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "Couldn't load news right now");
        }
    }

    /** RSS → trusted items, newest first, one per headline. */
    static List<NewsItem> parse(byte[] xml) throws Exception {
        DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();
        f.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        f.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true); // no XXE
        Document doc = f.newDocumentBuilder().parse(new ByteArrayInputStream(xml));

        List<NewsItem> out = new ArrayList<>();
        Set<String> seen = new java.util.HashSet<>();
        NodeList items = doc.getElementsByTagName("item");
        for (int i = 0; i < items.getLength(); i++) {
            Element item = (Element) items.item(i);
            Element src = first(item, "source");
            if (src == null || !trusted(src.getAttribute("url"))) continue;
            String source = src.getTextContent().trim();
            String title = stripSource(text(item, "title"), source);
            if (title.isEmpty() || !seen.add(title.toLowerCase())) continue;
            Instant at = null;
            try { at = ZonedDateTime.parse(text(item, "pubDate"), DateTimeFormatter.RFC_1123_DATE_TIME).toInstant(); }
            catch (Exception ignored) { }
            out.add(new NewsItem(title, source, text(item, "link"), at));
        }
        out.sort(Comparator.comparing(NewsItem::publishedAt, Comparator.nullsLast(Comparator.reverseOrder())));
        return out.size() > MAX_ITEMS ? out.subList(0, MAX_ITEMS) : out;
    }

    /** "RELIANCE INDUSTRIES LTD" → "Reliance Industries"; falls back to the symbol. */
    static String searchName(Instrument inst) {
        String n = inst.getName() == null ? "" : inst.getName();
        n = n.replaceAll("(?i)\\b(ltd|limited|ltd\\.|pvt|private|corporation|corp)\\b\\.?", "")
             .replaceAll("[^A-Za-z0-9&' ]", " ").replaceAll("\\s+", " ").trim();
        if (n.isEmpty()) return inst.getSymbol();
        if (n.equals(n.toUpperCase())) { // title-case shouty names so the query reads naturally
            StringBuilder b = new StringBuilder();
            for (String w : n.toLowerCase().split(" ")) b.append(Character.toUpperCase(w.charAt(0))).append(w.substring(1)).append(' ');
            n = b.toString().trim();
        }
        return n;
    }

    static boolean trusted(String url) {
        if (url == null) return false;
        String host = url.replaceFirst("^https?://", "").replaceFirst("/.*$", "").toLowerCase();
        return TRUSTED.stream().anyMatch(t -> host.equals(t) || host.endsWith("." + t));
    }

    /** Google appends " - Source" to every title; drop it. */
    static String stripSource(String title, String source) {
        String suffix = " - " + source;
        return title.endsWith(suffix) ? title.substring(0, title.length() - suffix.length()).trim() : title.trim();
    }

    private static Element first(Element parent, String tag) {
        NodeList l = parent.getElementsByTagName(tag);
        return l.getLength() == 0 ? null : (Element) l.item(0);
    }

    private static String text(Element parent, String tag) {
        Element e = first(parent, tag);
        return e == null ? "" : e.getTextContent();
    }
}
