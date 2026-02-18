package com.traderplanner.appbackend.service;

import com.traderplanner.appbackend.dto.PatternResponse;
import com.traderplanner.appbackend.dto.SecurityDto;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Deterministic fallback when OpenAI is unavailable or rate-limited. Picks
 * ticker by day-of-year + timeframe hash; rotates through allowed patterns.
 */
@Component
public class FallbackStrategy {

	private static final String[] ALLOWED_PATTERNS = {"Ascending Triangle", "Descending Triangle", "Bull Flag",
			"Bear Flag", "Cup and Handle", "Double Bottom", "Double Top", "Falling Wedge", "Rising Wedge",
			"Head and Shoulders"};

	/**
	 * Build a deterministic fallback response. Ticker and pattern are chosen from a
	 * single seed (day + timeframe) using remainder and quotient so that (ticker,
	 * pattern) gets full combinatorial variety instead of being limited to
	 * LCM(tickerCount, patternCount) when both used the same modulus.
	 */
	public PatternResponse buildFallback(String timeframe, List<SecurityDto> whitelist, String source) {
		String tf = "1d".equalsIgnoreCase(timeframe) ? "1d" : "1h";
		List<String> tickers = whitelist.stream().map(SecurityDto::getTicker).sorted().collect(Collectors.toList());
		if (tickers.isEmpty()) {
			tickers = List.of("AAPL");
		}
		int dayOfYear = java.time.LocalDate.now(java.time.ZoneOffset.UTC).getDayOfYear();
		int tfHash = "1d".equals(tf) ? 1 : 0;
		int seed = dayOfYear * 2 + tfHash;
		int tickerIdx = Math.floorMod(seed, tickers.size());
		int patternIdx = Math.floorMod(seed / tickers.size(), ALLOWED_PATTERNS.length);
		String ticker = tickers.get(tickerIdx);
		String pattern = ALLOWED_PATTERNS[patternIdx];
		String rationale = "Deterministic fallback selection (no AI). Consider setting OPENAI_API_KEY for live analysis.";
		return new PatternResponse(tf, ticker, pattern, rationale, OffsetDateTime.now(), source);
	}

	public static List<String> getAllowedPatterns() {
		return List.of(ALLOWED_PATTERNS);
	}

	public static boolean isAllowedPattern(String pattern) {
		if (pattern == null || pattern.isBlank())
			return false;
		String normalized = pattern.trim();
		for (String p : ALLOWED_PATTERNS) {
			if (p.equalsIgnoreCase(normalized))
				return true;
		}
		return false;
	}
}
