package com.traderplanner.appbackend.service;

import java.math.BigDecimal;

/**
 * One OHLCV bar: t (timestamp ISO), o,h,l,c,v.
 */
public record Candle(String t, BigDecimal o, BigDecimal h, BigDecimal l, BigDecimal c, long v) {
}
