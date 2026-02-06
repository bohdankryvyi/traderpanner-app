package com.traderplanner.appbackend.service;

import java.util.List;

/**
 * Provides OHLC candle data for a ticker and timeframe.
 * tf=1d: daily candles (e.g. last ~120 days).
 * tf=1h: intraday 60min candles (e.g. last ~200 bars).
 */
public interface MarketDataProvider {

    /**
     * Fetch candles. Returns empty list on failure (caller can treat as no data).
     * @param ticker symbol
     * @param tf "1h" or "1d"
     * @return list of {t, o, h, l, c, v} in chronological order (oldest first)
     */
    List<Candle> getCandles(String ticker, String tf);
}
