package com.traderplanner.appbackend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.traderplanner.appbackend.config.AiProperties;
import com.traderplanner.appbackend.dto.PatternResponse;
import com.traderplanner.appbackend.dto.SecurityDto;
import com.traderplanner.appbackend.exception.BadRequestException;
import com.traderplanner.appbackend.exception.ServiceUnavailableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class PatternService {

    private static final Logger log = LoggerFactory.getLogger(PatternService.class);
    private static final int MAX_TICKERS_FETCH = 5;
    private static final int TOP_PREFILTER = 3;
    private static final long DELAY_MS = 350;
    private static final int MIN_CANDLES = 10;
    private static final int CANDLES_1H = 30;
    private static final int CANDLES_1D = 60;
    /** Volatility (std of returns) must be in this range to be "moderate". */
    private static final double VOL_MIN = 0.0005;
    private static final double VOL_MAX = 0.05;

    private final SecurityService securityService;
    private final MarketDataProvider marketDataProvider;
    private final OpenAiClient openAiClient;
    private final AiProperties aiProperties;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final Map<String, CachedPattern> cache = new ConcurrentHashMap<>();

    public PatternService(SecurityService securityService,
                         MarketDataProvider marketDataProvider,
                         OpenAiClient openAiClient,
                         AiProperties aiProperties) {
        this.securityService = securityService;
        this.marketDataProvider = marketDataProvider;
        this.openAiClient = openAiClient;
        this.aiProperties = aiProperties;
    }

    public PatternResponse getPattern(String tf) {
        String openAiKey = System.getenv("OPENAI_API_KEY");
        if (openAiKey == null || openAiKey.isBlank()) {
            throw new BadRequestException("OPENAI_API_KEY is not configured");
        }
        String avKey = System.getenv("ALPHAVANTAGE_API_KEY");
        if (avKey == null || avKey.isBlank()) {
            throw new ServiceUnavailableException("Market data provider is not configured (ALPHAVANTAGE_API_KEY)");
        }
        if (!"1h".equalsIgnoreCase(tf) && !"1d".equalsIgnoreCase(tf)) {
            throw new BadRequestException("Unsupported timeframe: " + tf + " (expected 1h or 1d)");
        }

        String cacheKey = "pattern|" + tf.toLowerCase();
        CachedPattern cached = cache.get(cacheKey);
        OffsetDateTime now = OffsetDateTime.now();
        if (cached != null && ChronoUnit.MINUTES.between(cached.generatedAt(), now) < aiProperties.getCacheTtlMinutes()) {
            return cached.response();
        }

        List<SecurityDto> whitelist = securityService.findSecurities(null);
        if (whitelist.isEmpty()) {
            throw new ServiceUnavailableException("No securities in whitelist");
        }
        List<SecurityDto> sorted = whitelist.stream()
                .sorted(Comparator.comparing(SecurityDto::getTicker))
                .toList();
        int whitelistSize = sorted.size();
        long epochDay = LocalDate.now(ZoneOffset.UTC).toEpochDay();
        int offset = (int) Math.floorMod(epochDay, whitelistSize);
        List<String> tickersToFetch = new ArrayList<>();
        for (int i = 0; i < MAX_TICKERS_FETCH; i++) {
            int idx = (offset + i) % whitelistSize;
            tickersToFetch.add(sorted.get(idx).getTicker());
        }
        if (log.isDebugEnabled()) {
            log.debug("Candidate tickers (day offset={}): {}", offset, tickersToFetch);
        }

        List<TickerCandles> withData = new ArrayList<>();
        for (int i = 0; i < tickersToFetch.size(); i++) {
            if (i > 0) {
                try {
                    Thread.sleep(DELAY_MS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new ServiceUnavailableException("Interrupted while fetching market data", e);
                }
            }
            String ticker = tickersToFetch.get(i);
            List<Candle> candles = marketDataProvider.getCandles(ticker, tf);
            if (candles != null && candles.size() >= MIN_CANDLES) {
                withData.add(new TickerCandles(ticker, candles));
                if (log.isDebugEnabled()) {
                    log.debug("Fetched candles for {} tf={} bars={}", ticker, tf, candles.size());
                }
            }
        }

        if (withData.isEmpty()) {
            log.debug("No OHLC data for any ticker; rate limit or network");
            throw new ServiceUnavailableException("Market data fetch failed. Check ALPHAVANTAGE_API_KEY and rate limits.");
        }

        List<TickerCandles> top3 = prefilterTop3(withData);
        if (top3.size() < TOP_PREFILTER) {
            throw new ServiceUnavailableException("Not enough market data to analyze right now.");
        }

        List<String> allowedTickers = top3.stream().map(TickerCandles::ticker).toList();
        if (log.isDebugEnabled()) {
            log.debug("Top 3 candidates for OpenAI: {}", allowedTickers);
        }

        int nCandles = "1h".equalsIgnoreCase(tf) ? CANDLES_1H : CANDLES_1D;
        String userContent = buildUserMessage(tf, top3, nCandles);
        String systemContent = "You are a technical analysis expert. Output ONLY valid JSON with exactly these keys: \"timeframe\" (\"1h\" or \"1d\"), \"ticker\" (must be one of the 3 tickers provided), \"pattern\" (exactly one of: ascending triangle, descending triangle, bull flag, bear flag, double bottom, double top, range breakout forming), \"rationale\" (2-4 sentences referencing the given candles). No other text or keys.";
        List<Map<String, String>> messages = List.of(
                Map.of("role", "system", "content", systemContent),
                Map.of("role", "user", "content", userContent)
        );

        String content;
        try {
            content = openAiClient.chat(messages, openAiKey);
        } catch (Exception e) {
            log.debug("OpenAI call failed: {}", e.getMessage());
            throw new ServiceUnavailableException("AI unavailable: " + e.getMessage(), e);
        }

        JsonNode node;
        try {
            node = objectMapper.readTree(content);
        } catch (Exception e) {
            log.debug("OpenAI response not valid JSON");
            throw new ServiceUnavailableException("AI returned invalid response.");
        }
        String ticker = node.path("ticker").asText(null);
        String pattern = node.path("pattern").asText(null);
        String rationale = node.path("rationale").asText(null);
        if (ticker == null || ticker.isBlank() || pattern == null || pattern.isBlank() || rationale == null || rationale.isBlank()) {
            throw new ServiceUnavailableException("AI returned invalid response.");
        }
        String tickerUpper = ticker.trim().toUpperCase();
        if (!allowedTickers.stream().anyMatch(t -> t.equalsIgnoreCase(tickerUpper))) {
            throw new ServiceUnavailableException("AI returned invalid response.");
        }

        PatternResponse response = new PatternResponse(tf.toLowerCase(), tickerUpper, pattern.trim(), rationale.trim(), now);
        cache.put(cacheKey, new CachedPattern(response, now));
        return response;
    }

    /**
     * Filter: positive momentum and moderate volatility. Sort by volatility (prefer middle), take top 3.
     */
    private List<TickerCandles> prefilterTop3(List<TickerCandles> withData) {
        List<Scored> scored = new ArrayList<>();
        for (TickerCandles tc : withData) {
            List<Candle> c = tc.candles();
            if (c.size() < 2) continue;
            BigDecimal lastClose = c.get(c.size() - 1).c();
            BigDecimal prevClose = c.get(c.size() - 2).c();
            if (lastClose == null || prevClose == null || prevClose.compareTo(BigDecimal.ZERO) <= 0) continue;
            double momentum = lastClose.subtract(prevClose).divide(prevClose, 6, RoundingMode.HALF_UP).doubleValue();
            if (momentum <= 0) continue;
            double vol = computeVolatility(c);
            if (vol < VOL_MIN || vol > VOL_MAX) continue;
            scored.add(new Scored(tc, vol, momentum));
        }
        return scored.stream()
                .sorted(Comparator.comparingDouble((Scored s) -> Math.abs(s.volatility() - (VOL_MIN + VOL_MAX) / 2))) // prefer middle volatility
                .limit(TOP_PREFILTER)
                .map(Scored::tickerCandles)
                .toList();
    }

    private static double computeVolatility(List<Candle> candles) {
        if (candles == null || candles.size() < 2) return 0;
        double sum = 0;
        int n = 0;
        for (int i = 1; i < candles.size(); i++) {
            BigDecimal c0 = candles.get(i - 1).c();
            BigDecimal c1 = candles.get(i).c();
            if (c0 != null && c1 != null && c0.compareTo(BigDecimal.ZERO) > 0) {
                double ret = c1.subtract(c0).divide(c0, 6, RoundingMode.HALF_UP).doubleValue();
                sum += ret * ret;
                n++;
            }
        }
        return n > 0 ? Math.sqrt(sum / n) : 0;
    }

    private String buildUserMessage(String tf, List<TickerCandles> top3, int nCandles) {
        StringBuilder sb = new StringBuilder();
        sb.append("Timeframe: ").append(tf).append("\n\n");
        sb.append("For each ticker, last ").append(nCandles).append(" candles as [timestamp, open, high, low, close]. Pick the single best ticker and one pattern from the list. Return JSON only.\n\n");
        for (TickerCandles tc : top3) {
            List<Candle> list = tc.candles();
            int from = Math.max(0, list.size() - nCandles);
            list = list.subList(from, list.size());
            sb.append("Ticker: ").append(tc.ticker()).append("\n");
            sb.append("Candles: ");
            sb.append(list.stream()
                    .map(c -> String.format("[%s,%s,%s,%s,%s]", c.t(), c.o(), c.h(), c.l(), c.c()))
                    .collect(Collectors.joining(", ")));
            sb.append("\n\n");
        }
        return sb.toString();
    }

    private record TickerCandles(String ticker, List<Candle> candles) {}
    private record Scored(TickerCandles tickerCandles, double volatility, double momentum) {}
    private record CachedPattern(PatternResponse response, OffsetDateTime generatedAt) {}
}
