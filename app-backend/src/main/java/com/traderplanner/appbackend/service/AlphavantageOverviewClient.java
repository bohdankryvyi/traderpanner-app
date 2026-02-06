package com.traderplanner.appbackend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Fetches Company OVERVIEW from Alpha Vantage. Returns EPS and P/E for ranking.
 * If key is missing or request fails, callers should fall back to deterministic logic.
 */
@Component
public class AlphavantageOverviewClient {

    private static final String OVERVIEW_URL = "https://www.alphavantage.co/query?function=OVERVIEW&symbol=%s&apikey=%s";
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(10);

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(REQUEST_TIMEOUT)
            .build();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public record OverviewData(double eps, double pe) {}

    /**
     * Returns null if request fails or required fields are missing/None.
     */
    public OverviewData fetchOverview(String symbol, String apiKey) {
        if (symbol == null || symbol.isBlank() || apiKey == null || apiKey.isBlank()) {
            return null;
        }
        try {
            String url = String.format(OVERVIEW_URL, symbol, apiKey);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(REQUEST_TIMEOUT)
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) return null;

            JsonNode root = objectMapper.readTree(response.body());
            if (root == null || !root.isObject()) return null;

            String epsStr = getText(root, "EPS");
            String peStr = getText(root, "PERatio");
            if (epsStr == null && peStr == null) return null;

            double eps = parseDouble(epsStr, 0.0);
            double pe = parseDouble(peStr, Double.MAX_VALUE);
            return new OverviewData(eps, pe);
        } catch (Exception e) {
            return null;
        }
    }

    private static String getText(JsonNode node, String key) {
        JsonNode n = node.get(key);
        if (n == null || !n.isTextual()) return null;
        String s = n.asText();
        if (s == null || s.isBlank() || "None".equalsIgnoreCase(s)) return null;
        return s;
    }

    private static double parseDouble(String s, double defaultValue) {
        if (s == null || s.isBlank() || "None".equalsIgnoreCase(s)) return defaultValue;
        try {
            return Double.parseDouble(s.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}
