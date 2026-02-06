package com.traderplanner.appbackend.service;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Service
public class PriceService {

    private static final long QUOTE_DELAY_MS = 250;

    private final AlphavantageQuoteClient quoteClient;

    public PriceService(AlphavantageQuoteClient quoteClient) {
        this.quoteClient = quoteClient;
    }

    /**
     * Deterministic pseudo-price generator based on ticker (fallback when no API key or API fails).
     */
    public BigDecimal getPriceUsd(String ticker) {
        if (ticker == null || ticker.isBlank()) {
            return BigDecimal.ZERO;
        }
        int hash = Math.abs(ticker.toUpperCase().hashCode());
        BigDecimal base = BigDecimal.valueOf(hash % 50000).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        return base.add(BigDecimal.TEN);
    }

    public Map<String, BigDecimal> getPricesUsd(Set<String> tickers) {
        Map<String, BigDecimal> result = new HashMap<>();
        String apiKey = System.getenv("ALPHAVANTAGE_API_KEY");
        if (apiKey != null && !apiKey.isBlank()) {
            for (String t : tickers) {
                try {
                    if (result.size() > 0) {
                        Thread.sleep(QUOTE_DELAY_MS);
                    }
                    BigDecimal price = quoteClient.fetchPriceUsd(t, apiKey);
                    result.put(t, price != null ? price.setScale(2, RoundingMode.HALF_UP) : getPriceUsd(t));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    result.put(t, getPriceUsd(t));
                } catch (Exception e) {
                    result.put(t, getPriceUsd(t));
                }
            }
        } else {
            for (String t : tickers) {
                result.put(t, getPriceUsd(t));
            }
        }
        return result;
    }
}

