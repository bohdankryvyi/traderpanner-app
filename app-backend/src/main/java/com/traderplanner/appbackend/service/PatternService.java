package com.traderplanner.appbackend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.traderplanner.appbackend.config.AiProperties;
import com.traderplanner.appbackend.dto.PatternResponse;
import com.traderplanner.appbackend.dto.SecurityDto;
import com.traderplanner.appbackend.exception.BadRequestException;
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
import java.util.concurrent.atomic.AtomicInteger;
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
    private static final double VOL_MIN = 0.0005;
    private static final double VOL_MAX = 0.05;
    private static final int RATIONALE_MIN = 20;
    private static final int RATIONALE_MAX = 600;

    private static final String SYSTEM_PROMPT = "You are a technical analysis expert. Return ONLY valid JSON. No markdown, no code fences. "
            + "JSON must have exactly these keys: \"timeframe\" (\"1h\" or \"1d\"), \"ticker\" (must be one of the tickers provided), "
            + "\"pattern\" (exactly one of: Ascending Triangle, Descending Triangle, Bull Flag, Bear Flag, Cup and Handle, Double Bottom, Double Top, Falling Wedge, Rising Wedge, Head and Shoulders), "
            + "\"rationale\" (2-4 sentences, 20-600 characters).";

    private final SecurityService securityService;
    private final MarketDataProvider marketDataProvider;
    private final OpenAiClient openAiClient;
    private final AiProperties aiProperties;
    private final FallbackStrategy fallbackStrategy;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final Map<String, CachedPattern> cache = new ConcurrentHashMap<>();
    private final Map<String, AtomicInteger> dailyCount = new ConcurrentHashMap<>();

    public PatternService(SecurityService securityService,
                          MarketDataProvider marketDataProvider,
                          OpenAiClient openAiClient,
                          AiProperties aiProperties,
                          FallbackStrategy fallbackStrategy) {
        this.securityService = securityService;
        this.marketDataProvider = marketDataProvider;
        this.openAiClient = openAiClient;
        this.aiProperties = aiProperties;
        this.fallbackStrategy = fallbackStrategy;
    }

    public PatternResponse getPattern(String tf) {
        if (!"1h".equalsIgnoreCase(tf) && !"1d".equalsIgnoreCase(tf)) {
            throw new BadRequestException("Unsupported timeframe: " + tf + " (expected 1h or 1d)");
        }
        String timeframe = tf.toLowerCase();
        OffsetDateTime now = OffsetDateTime.now();

        String cacheKey = "pattern|" + timeframe;
        CachedPattern cached = cache.get(cacheKey);
        if (cached != null && ChronoUnit.MINUTES.between(cached.generatedAt(), now) < aiProperties.getCacheTtlMinutes()) {
            return cached.response();
        }

        // Evict stale day keys on every cache miss so dailyCount never grows unbounded
        evictStaleDailyCounts();

        List<SecurityDto> whitelist = securityService.findSecurities(null);
        if (whitelist.isEmpty()) {
            whitelist = List.of(new com.traderplanner.appbackend.dto.SecurityDto("AAPL", "Apple Inc", "Technology"));
        }
        List<SecurityDto> sorted = whitelist.stream().sorted(Comparator.comparing(SecurityDto::getTicker)).toList();

        String openAiKey = System.getenv("OPENAI_API_KEY");
        if (openAiKey == null || openAiKey.isBlank()) {
            return cacheAndReturn(cacheKey, fallbackStrategy.buildFallback(timeframe, sorted, "fallback"), now);
        }
        if (!aiProperties.isEnabled()) {
            return cacheAndReturn(cacheKey, fallbackStrategy.buildFallback(timeframe, sorted, "fallback"), now);
        }

        // Optional fast path: skip heavy work if we're already at limit (racy is ok; reserve is enforced below)
        if (dailyCountExceeded()) {
            return cacheAndReturn(cacheKey, fallbackStrategy.buildFallback(timeframe, sorted, "fallback-rate-limit"), now);
        }

        List<String> candidateTickers = buildCandidateTickers(sorted, timeframe);
        List<TickerCandles> withCandles = fetchCandlesForCandidates(candidateTickers, timeframe);
        List<String> allowedTickers = allowedTickersFromCandidates(candidateTickers, withCandles);
        boolean hasMarketData = !withCandles.isEmpty();

        if (allowedTickers.isEmpty()) {
            log.debug("No allowed tickers after prefilter (no positive momentum + moderate volatility); returning fallback");
            return cacheAndReturn(cacheKey, fallbackStrategy.buildFallback(timeframe, sorted, "fallback"), now);
        }

        // Only include candle data for allowed tickers so prompt and validation stay in sync
        List<TickerCandles> candlesForPrompt = withCandles.stream()
                .filter(tc -> allowedTickers.stream().anyMatch(t -> t.equalsIgnoreCase(tc.ticker())))
                .toList();

        String userContent = buildUserMessage(timeframe, allowedTickers, candlesForPrompt);
        List<Map<String, String>> messages = List.of(
                Map.of("role", "system", "content", SYSTEM_PROMPT),
                Map.of("role", "user", "content", userContent)
        );

        // Atomic reserve: only allow up to maxRequestsPerDay to call OpenAI (closes race with concurrent requests)
        if (!tryReserveDailySlot()) {
            return cacheAndReturn(cacheKey, fallbackStrategy.buildFallback(timeframe, sorted, "fallback-rate-limit"), now);
        }

        try {
            String content = openAiClient.chat(messages, openAiKey, aiProperties.getModel());
            JsonNode node = objectMapper.readTree(content);
            String ticker = node.path("ticker").asText(null);
            String pattern = node.path("pattern").asText(null);
            String rationale = node.path("rationale").asText(null);
            if (ticker == null || ticker.isBlank() || pattern == null || pattern.isBlank() || rationale == null || rationale.isBlank()) {
                log.debug("OpenAI response missing required fields");
                return cacheAndReturn(cacheKey, fallbackStrategy.buildFallback(timeframe, sorted, "fallback-openai-invalid"), now);
            }
            String tickerUpper = ticker.trim().toUpperCase();
            if (!allowedTickers.stream().anyMatch(t -> t.equalsIgnoreCase(tickerUpper))) {
                log.debug("OpenAI ticker not in whitelist: {}", tickerUpper);
                return cacheAndReturn(cacheKey, fallbackStrategy.buildFallback(timeframe, sorted, "fallback-openai-invalid"), now);
            }
            if (!FallbackStrategy.isAllowedPattern(pattern)) {
                log.debug("OpenAI pattern not allowed: {}", pattern);
                return cacheAndReturn(cacheKey, fallbackStrategy.buildFallback(timeframe, sorted, "fallback-openai-invalid"), now);
            }
            int len = rationale.trim().length();
            if (len < RATIONALE_MIN || len > RATIONALE_MAX) {
                log.debug("OpenAI rationale length invalid: {}", len);
                return cacheAndReturn(cacheKey, fallbackStrategy.buildFallback(timeframe, sorted, "fallback-openai-invalid"), now);
            }
            String source = hasMarketData ? "openai" : "openai-no-market-data";
            PatternResponse response = new PatternResponse(timeframe, tickerUpper, normalizePattern(pattern), rationale.trim(), now, source);
            return cacheAndReturn(cacheKey, response, now);
        } catch (Exception e) {
            log.warn("OpenAI call failed: {}", e.getMessage(), e);
            PatternResponse fallback = fallbackStrategy.buildFallback(timeframe, sorted, "fallback-openai-error");
            fallback.setRationale("AI unavailable, showing fallback.");
            return cacheAndReturn(cacheKey, fallback, now);
        }
    }

    /**
     * Removes day keys other than today so {@link #dailyCount} does not grow unbounded over time.
     */
    private void evictStaleDailyCounts() {
        String today = LocalDate.now(ZoneOffset.UTC).toString();
        dailyCount.keySet().removeIf(key -> !today.equals(key));
    }

    /**
     * Fast path check (racy); real enforcement is {@link #tryReserveDailySlot()}.
     */
    private boolean dailyCountExceeded() {
        evictStaleDailyCounts();
        String dayKey = LocalDate.now(ZoneOffset.UTC).toString();
        dailyCount.putIfAbsent(dayKey, new AtomicInteger(0));
        return dailyCount.get(dayKey).get() >= aiProperties.getMaxRequestsPerDay();
    }

    /**
     * Atomically reserves one slot for today if under maxRequestsPerDay. Returns true if reserved, false if limit reached.
     * Call immediately before the OpenAI request so concurrent requests cannot exceed the limit.
     */
    private boolean tryReserveDailySlot() {
        evictStaleDailyCounts();
        String dayKey = LocalDate.now(ZoneOffset.UTC).toString();
        AtomicInteger counter = dailyCount.computeIfAbsent(dayKey, k -> new AtomicInteger(0));
        int max = aiProperties.getMaxRequestsPerDay();
        for (;;) {
            int current = counter.get();
            if (current >= max) return false;
            if (counter.compareAndSet(current, current + 1)) return true;
        }
    }

    private PatternResponse cacheAndReturn(String cacheKey, PatternResponse response, OffsetDateTime now) {
        cache.put(cacheKey, new CachedPattern(response, now));
        return response;
    }

    private List<String> buildCandidateTickers(List<SecurityDto> sorted, String timeframe) {
        int size = sorted.size();
        long epochDay = LocalDate.now(ZoneOffset.UTC).toEpochDay();
        int offset = (int) Math.floorMod(epochDay, size);
        List<String> out = new ArrayList<>();
        for (int i = 0; i < MAX_TICKERS_FETCH; i++) {
            int idx = (offset + i) % size;
            out.add(sorted.get(idx).getTicker());
        }
        return out;
    }

    private List<TickerCandles> fetchCandlesForCandidates(List<String> tickers, String tf) {
        List<TickerCandles> withData = new ArrayList<>();
        for (int i = 0; i < tickers.size(); i++) {
            if (i > 0) {
                try {
                    Thread.sleep(DELAY_MS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
            String ticker = tickers.get(i);
            List<Candle> candles = marketDataProvider.getCandles(ticker, tf);
            if (candles != null && candles.size() >= MIN_CANDLES) {
                withData.add(new TickerCandles(ticker, candles));
            }
        }
        return withData;
    }

    /**
     * Returns tickers allowed for OpenAI: when we have candle data, only tickers that pass
     * the positive-momentum and moderate-volatility prefilter; when we have no candle data,
     * returns all candidates (no validation possible).
     */
    private List<String> allowedTickersFromCandidates(List<String> candidateTickers, List<TickerCandles> withCandles) {
        if (withCandles.isEmpty()) {
            return candidateTickers;
        }
        List<TickerCandles> filtered = prefilterTop3(withCandles);
        return filtered.stream().map(TickerCandles::ticker).toList();
    }

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
                .sorted(Comparator.comparingDouble((Scored s) -> Math.abs(s.volatility() - (VOL_MIN + VOL_MAX) / 2)))
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

    private String buildUserMessage(String tf, List<String> allowedTickers, List<TickerCandles> withCandles) {
        StringBuilder sb = new StringBuilder();
        sb.append("Timeframe: ").append(tf).append("\n\n");
        sb.append("Candidate tickers: ").append(String.join(", ", allowedTickers)).append("\n\n");
        int nCandles = "1d".equalsIgnoreCase(tf) ? CANDLES_1D : CANDLES_1H;
        if (!withCandles.isEmpty()) {
            sb.append("Last ").append(nCandles).append(" candles per ticker [timestamp, open, high, low, close]. Pick the single best ticker and one pattern from the allowed list. Return JSON only.\n\n");
            for (TickerCandles tc : withCandles) {
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
        } else {
            sb.append("No candle data available. Choose the single best ticker from the candidate list and one pattern. Return JSON only.");
        }
        return sb.toString();
    }

    private static String normalizePattern(String pattern) {
        for (String p : FallbackStrategy.getAllowedPatterns()) {
            if (p.equalsIgnoreCase(pattern.trim())) return p;
        }
        return pattern.trim();
    }

    private record TickerCandles(String ticker, List<Candle> candles) {}
    private record Scored(TickerCandles tickerCandles, double volatility, double momentum) {}
    private record CachedPattern(PatternResponse response, OffsetDateTime generatedAt) {}
}
