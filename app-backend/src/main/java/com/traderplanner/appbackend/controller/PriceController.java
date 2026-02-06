package com.traderplanner.appbackend.controller;

import com.traderplanner.appbackend.exception.BadRequestException;
import com.traderplanner.appbackend.service.PriceService;
import com.traderplanner.appbackend.service.SecurityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/prices")
@Tag(name = "Prices")
public class PriceController {

    private final PriceService priceService;
    private final SecurityService securityService;

    public PriceController(PriceService priceService, SecurityService securityService) {
        this.priceService = priceService;
        this.securityService = securityService;
    }

    @GetMapping
    @Operation(summary = "Get mock prices for tickers")
    public Map<String, BigDecimal> getPrices(@RequestParam("tickers") String tickersParam) {
        if (tickersParam == null || tickersParam.isBlank()) {
            throw new BadRequestException("tickers parameter is required");
        }

        Set<String> tickers = Arrays.stream(tickersParam.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toCollection(LinkedHashSet::new));

        if (tickers.isEmpty()) {
            throw new BadRequestException("No valid tickers provided");
        }

        List<String> unknown = new ArrayList<>();
        for (String t : tickers) {
            try {
                securityService.assertTickerExists(t);
            } catch (BadRequestException e) {
                unknown.add(t);
            }
        }

        if (!unknown.isEmpty()) {
            throw new BadRequestException("Unknown tickers: " + String.join(", ", unknown));
        }

        return priceService.getPricesUsd(tickers);
    }
}

