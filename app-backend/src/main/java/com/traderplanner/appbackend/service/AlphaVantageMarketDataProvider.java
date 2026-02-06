package com.traderplanner.appbackend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.traderplanner.appbackend.config.AiProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class AlphaVantageMarketDataProvider implements MarketDataProvider {

    private static final Logger log = LoggerFactory.getLogger(AlphaVantageMarketDataProvider.class);
    private static final String DAILY_URL = "https://www.alphavantage.co/query?function=TIME_SERIES_DAILY_ADJUSTED&symbol=%s&apikey=%s&outputsize=compact";
    private static final String INTRADAY_URL = "https://www.alphavantage.co/query?function=TIME_SERIES_INTRADAY&symbol=%s&interval=60min&apikey=%s&outputsize=compact";
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(15);
    private static final int MAX_DAILY_BARS = 120;
    private static final int MAX_INTRADAY_BARS = 200;

    private final HttpClient httpClient = HttpClient.newBuilder().connectTimeout(REQUEST_TIMEOUT).build();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final AiProperties aiProperties;

    private final Map<String, CachedCandles> cache = new ConcurrentHashMap<>();

    public AlphaVantageMarketDataProvider(AiProperties aiProperties) {
        this.aiProperties = aiProperties;
    }

    @Override
    public List<Candle> getCandles(String ticker, String tf) {
        String key = ticker + "|" + tf;
        CachedCandles cached = cache.get(key);
        if (cached != null && (System.currentTimeMillis() - cached.cachedAt()) < aiProperties.getCacheTtlMinutes() * 60_000L) {
            if (log.isDebugEnabled()) {
                log.debug("AlphaVantage cache hit for {} tf={}", ticker, tf);
            }
            return cached.candles();
        }

        List<Candle> candles = fetchCandles(ticker, tf);
        if (!candles.isEmpty()) {
            cache.put(key, new CachedCandles(candles, System.currentTimeMillis()));
            if (log.isDebugEnabled()) {
                log.debug("AlphaVantage fetch ok for {} tf={}, bars={}", ticker, tf, candles.size());
            }
        } else {
            if (log.isDebugEnabled()) {
                log.debug("AlphaVantage fetch empty/failed for {} tf={}", ticker, tf);
            }
        }
        return candles;
    }

    private List<Candle> fetchCandles(String ticker, String tf) {
        String apiKey = System.getenv("ALPHAVANTAGE_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            return List.of();
        }
        String url = "1d".equalsIgnoreCase(tf)
                ? String.format(DAILY_URL, ticker, apiKey)
                : String.format(INTRADAY_URL, ticker, apiKey);
        String seriesKey = "1d".equalsIgnoreCase(tf) ? "Time Series (Daily)" : "Time Series (60min)";
        int maxBars = "1d".equalsIgnoreCase(tf) ? MAX_DAILY_BARS : MAX_INTRADAY_BARS;

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(REQUEST_TIMEOUT)
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                log.debug("AlphaVantage status {} for {} tf={}", response.statusCode(), ticker, tf);
                return List.of();
            }
            JsonNode root = objectMapper.readTree(response.body());
            if (root == null) return List.of();
            if (root.has("Information") || root.has("Note")) {
                log.debug("AlphaVantage rate limit or info for {} tf={}", ticker, tf);
                return List.of();
            }
            JsonNode series = root.get(seriesKey);
            if (series == null || !series.isObject()) return List.of();

            List<Candle> out = new ArrayList<>();
            Iterator<Map.Entry<String, JsonNode>> it = series.fields();
            while (it.hasNext() && out.size() < maxBars) {
                Map.Entry<String, JsonNode> e = it.next();
                String time = e.getKey();
                JsonNode node = e.getValue();
                BigDecimal o = getDecimal(node, "1. open");
                BigDecimal h = getDecimal(node, "2. high");
                BigDecimal l = getDecimal(node, "3. low");
                BigDecimal c = getDecimal(node, "4. close");
                long v = getLong(node, "6. volume");
                if (o != null && h != null && l != null && c != null) {
                    out.add(new Candle(time, o, h, l, c, v));
                }
            }
            out.sort((a, b) -> a.t().compareTo(b.t()));
            return out;
        } catch (Exception e) {
            log.debug("AlphaVantage fetch error for {} tf={}: {}", ticker, tf, e.getMessage());
            return List.of();
        }
    }

    private static BigDecimal getDecimal(JsonNode node, String key) {
        JsonNode n = node.get(key);
        if (n == null) return null;
        try {
            return new BigDecimal(n.asText().trim());
        } catch (Exception e) {
            return null;
        }
    }

    private static long getLong(JsonNode node, String key) {
        JsonNode n = node.get(key);
        if (n == null) return 0L;
        try {
            return Long.parseLong(n.asText().trim());
        } catch (Exception e) {
            return 0L;
        }
    }

    private record CachedCandles(List<Candle> candles, long cachedAt) {}
}
