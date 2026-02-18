package com.traderplanner.appbackend.service;

import com.traderplanner.appbackend.config.AiProperties;
import com.traderplanner.appbackend.dto.AiTipsResponse;
import com.traderplanner.appbackend.dto.SecurityDto;
import com.traderplanner.appbackend.exception.BadRequestException;
import com.traderplanner.appbackend.exception.ServiceUnavailableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ArrayList;

@Service
public class AiTipsService {

	private static final Logger log = LoggerFactory.getLogger(AiTipsService.class);
	private static final int ALPHAVANTAGE_MAX_TICKERS = 5;
	private static final long ALPHAVANTAGE_DELAY_MS = 300;

	public enum Timeframe {
		H1("1h"), D1("1d");

		private final String queryValue;

		Timeframe(String queryValue) {
			this.queryValue = queryValue;
		}

		public String toQueryValue() {
			return queryValue;
		}

		public static Timeframe fromQuery(String tf) {
			if ("1h".equalsIgnoreCase(tf)) {
				return H1;
			}
			if ("1d".equalsIgnoreCase(tf)) {
				return D1;
			}
			throw new BadRequestException("Unsupported timeframe: " + tf + " (expected 1h or 1d)");
		}
	}

	private static class CachedTips {
		private final AiTipsResponse response;
		private final OffsetDateTime generatedAt;

		private CachedTips(AiTipsResponse response, OffsetDateTime generatedAt) {
			this.response = response;
			this.generatedAt = generatedAt;
		}
	}

	private final AiProperties aiProperties;
	private final SecurityService securityService;
	private final AlphavantageOverviewClient alphavantageClient;

	private final Map<Timeframe, CachedTips> cache = new EnumMap<>(Timeframe.class);

	public AiTipsService(AiProperties aiProperties, SecurityService securityService,
			AlphavantageOverviewClient alphavantageClient) {
		this.aiProperties = aiProperties;
		this.securityService = securityService;
		this.alphavantageClient = alphavantageClient;
	}

	public AiTipsResponse getTips(String timeframeQuery) {
		if (System.getenv("OPENAI_API_KEY") == null || System.getenv("OPENAI_API_KEY").isBlank()) {
			throw new BadRequestException("OPENAI_API_KEY is not configured");
		}
		if (System.getenv("ALPHAVANTAGE_API_KEY") == null || System.getenv("ALPHAVANTAGE_API_KEY").isBlank()) {
			throw new ServiceUnavailableException("Market data provider is not configured (ALPHAVANTAGE_API_KEY)");
		}

		Timeframe timeframe = Timeframe.fromQuery(timeframeQuery);
		CachedTips cached = cache.get(timeframe);
		OffsetDateTime now = OffsetDateTime.now();
		if (cached != null && !isExpired(cached.generatedAt, now)) {
			return cached.response;
		}

		AiTipsResponse generated = generateTips(timeframe, now);
		cache.put(timeframe, new CachedTips(generated, now));
		return generated;
	}

	private boolean isExpired(OffsetDateTime generatedAt, OffsetDateTime now) {
		long minutes = ChronoUnit.MINUTES.between(generatedAt, now);
		return minutes >= aiProperties.getCacheTtlMinutes();
	}

	private AiTipsResponse generateTips(Timeframe timeframe, OffsetDateTime now) {
		List<SecurityDto> securities = securityService.findSecurities(null);
		if (securities.size() < 3) {
			throw new BadRequestException("Not enough securities seeded to generate AI tips");
		}

		String apiKey = System.getenv("ALPHAVANTAGE_API_KEY");
		try {
			return generateTipsWithAlphavantage(timeframe, now, securities, apiKey);
		} catch (ServiceUnavailableException e) {
			throw e;
		} catch (Exception e) {
			log.warn("Alpha Vantage tips failed: {}", e.getMessage());
			throw new ServiceUnavailableException("Market data unavailable: " + e.getMessage(), e);
		}
	}

	/**
	 * Fetch OVERVIEW for first tickers (alphabetically); take first 3 with data. No
	 * EPS/valuation ranking.
	 */
	private AiTipsResponse generateTipsWithAlphavantage(Timeframe timeframe, OffsetDateTime now,
			List<SecurityDto> securities, String apiKey) {
		List<SecurityDto> sorted = securities.stream().sorted(Comparator.comparing(SecurityDto::getTicker))
				.limit(ALPHAVANTAGE_MAX_TICKERS).toList();

		List<SecurityDto> withData = new ArrayList<>();
		for (SecurityDto s : sorted) {
			try {
				Thread.sleep(ALPHAVANTAGE_DELAY_MS);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				throw new ServiceUnavailableException("Interrupted", e);
			}
			AlphavantageOverviewClient.OverviewData data = alphavantageClient.fetchOverview(s.getTicker(), apiKey);
			if (data != null) {
				withData.add(s);
				if (withData.size() >= 3)
					break;
			}
		}

		if (withData.size() < 3) {
			throw new ServiceUnavailableException("Not enough market data to generate tips. Check rate limits.");
		}

		List<SecurityDto> top3 = withData.subList(0, 3);
		return buildTipsResponse(timeframe, now, top3);
	}

	private AiTipsResponse buildTipsResponse(Timeframe timeframe, OffsetDateTime now, List<SecurityDto> top3) {
		String horizon = timeframe == Timeframe.H1 ? "next 1 hour" : "next 1 day";
		SecurityDto s1 = top3.get(0);
		SecurityDto s2 = top3.get(1);
		SecurityDto s3 = top3.get(2);

		String text = String.format(Locale.ENGLISH,
				"For the %s, top 3 from whitelist (Alpha Vantage):%n%n" + "1. %s (%s) – %s%n" + "2. %s (%s) – %s%n"
						+ "3. %s (%s) – %s%n%n" + "From S&P 500 whitelist. Watchlist only; not financial advice.",
				horizon, s1.getTicker(), s1.getSector(), s1.getCompany(), s2.getTicker(), s2.getSector(),
				s2.getCompany(), s3.getTicker(), s3.getSector(), s3.getCompany());

		return new AiTipsResponse(timeframe.toQueryValue(), text, now);
	}
}
