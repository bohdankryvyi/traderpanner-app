package com.traderplanner.appbackend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Fetches GLOBAL_QUOTE from Alpha Vantage for real price. Returns null on
 * failure.
 */
@Component
public class AlphavantageQuoteClient {

	private static final String QUOTE_URL = "https://www.alphavantage.co/query?function=GLOBAL_QUOTE&symbol=%s&apikey=%s";
	private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(10);

	private final HttpClient httpClient = HttpClient.newBuilder().connectTimeout(REQUEST_TIMEOUT).build();
	private final ObjectMapper objectMapper = new ObjectMapper();

	/**
	 * Returns price in USD or null if request fails or "05. price" is missing.
	 */
	public BigDecimal fetchPriceUsd(String symbol, String apiKey) {
		if (symbol == null || symbol.isBlank() || apiKey == null || apiKey.isBlank()) {
			return null;
		}
		try {
			String url = String.format(QUOTE_URL, symbol, apiKey);
			HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).timeout(REQUEST_TIMEOUT).GET().build();
			HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
			if (response.statusCode() != 200)
				return null;

			JsonNode root = objectMapper.readTree(response.body());
			if (root == null)
				return null;
			JsonNode globalQuote = root.get("Global Quote");
			if (globalQuote == null || !globalQuote.isObject())
				return null;
			JsonNode priceNode = globalQuote.get("05. price");
			if (priceNode == null)
				return null;
			String priceStr = priceNode.asText();
			if (priceStr == null || priceStr.isBlank() || "None".equalsIgnoreCase(priceStr))
				return null;
			return new BigDecimal(priceStr.trim());
		} catch (Exception e) {
			return null;
		}
	}
}
