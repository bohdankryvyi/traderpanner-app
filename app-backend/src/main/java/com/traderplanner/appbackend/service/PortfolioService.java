package com.traderplanner.appbackend.service;

import com.traderplanner.appbackend.config.FxProperties;
import com.traderplanner.appbackend.domain.PortfolioPosition;
import com.traderplanner.appbackend.dto.PortfolioPositionRequest;
import com.traderplanner.appbackend.dto.PortfolioPositionResponse;
import com.traderplanner.appbackend.exception.NotFoundException;
import com.traderplanner.appbackend.repository.PortfolioPositionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class PortfolioService {

    private final PortfolioPositionRepository repository;
    private final SecurityService securityService;
    private final PriceService priceService;
    private final FxProperties fxProperties;

    public PortfolioService(PortfolioPositionRepository repository,
                            SecurityService securityService,
                            PriceService priceService,
                            FxProperties fxProperties) {
        this.repository = repository;
        this.securityService = securityService;
        this.priceService = priceService;
        this.fxProperties = fxProperties;
    }

    @Transactional(readOnly = true)
    public List<PortfolioPositionResponse> getAll() {
        List<PortfolioPosition> positions = repository.findAll();
        Set<String> tickers = positions.stream()
                .map(PortfolioPosition::getTicker)
                .collect(Collectors.toSet());

        Map<String, BigDecimal> prices = priceService.getPricesUsd(tickers);

        BigDecimal totalInvested = BigDecimal.ZERO;
        BigDecimal totalExpectedProfit = BigDecimal.ZERO;

        Map<Long, BigDecimal> investedPerPosition = new HashMap<>();
        Map<Long, BigDecimal> expectedProfitPerPosition = new HashMap<>();

        for (PortfolioPosition position : positions) {
            BigDecimal priceUsd = prices.getOrDefault(position.getTicker(), BigDecimal.ZERO);
            BigDecimal investedUsd = toUsd(position.getBuyPrice(), position.getCurrency())
                    .multiply(position.getQuantity());
            BigDecimal targetUsd = Optional.ofNullable(position.getTargetPrice())
                    .map(tp -> toUsd(tp, position.getCurrency()))
                    .orElse(priceUsd);
            BigDecimal expectedUsd = targetUsd.multiply(position.getQuantity());
            BigDecimal expectedProfitUsd = expectedUsd.subtract(investedUsd);

            investedPerPosition.put(position.getId(), investedUsd);
            expectedProfitPerPosition.put(position.getId(), expectedProfitUsd);

            totalInvested = totalInvested.add(investedUsd);
            totalExpectedProfit = totalExpectedProfit.add(expectedProfitUsd);
        }

        List<PortfolioPositionResponse> responses = new ArrayList<>();
        for (PortfolioPosition position : positions) {
            BigDecimal priceUsd = prices.getOrDefault(position.getTicker(), BigDecimal.ZERO);
            BigDecimal investedUsd = investedPerPosition.getOrDefault(position.getId(), BigDecimal.ZERO);
            BigDecimal targetUsd = Optional.ofNullable(position.getTargetPrice())
                    .map(tp -> toUsd(tp, position.getCurrency()))
                    .orElse(priceUsd);
            BigDecimal upsidePercent = computeUpside(priceUsd, targetUsd);
            BigDecimal expectedProfitUsd = expectedProfitPerPosition.getOrDefault(position.getId(), BigDecimal.ZERO);

            BigDecimal sharePercent = BigDecimal.ZERO;
            if (totalInvested.compareTo(BigDecimal.ZERO) > 0) {
                sharePercent = investedUsd
                        .multiply(BigDecimal.valueOf(100))
                        .divide(totalInvested, 4, RoundingMode.HALF_UP);
            }

            BigDecimal profitSharePercent = BigDecimal.ZERO;
            if (totalExpectedProfit.compareTo(BigDecimal.ZERO) != 0) {
                profitSharePercent = expectedProfitUsd
                        .multiply(BigDecimal.valueOf(100))
                        .divide(totalExpectedProfit, 4, RoundingMode.HALF_UP);
            }

            PortfolioPositionResponse dto = mapToResponse(position);
            dto.setCurrentPriceUsd(priceUsd.setScale(4, RoundingMode.HALF_UP));
            dto.setUpsidePercent(upsidePercent);
            dto.setInvestedAmountUsd(investedUsd.setScale(4, RoundingMode.HALF_UP));
            dto.setSharePercent(sharePercent);
            dto.setExpectedProfitUsd(expectedProfitUsd.setScale(4, RoundingMode.HALF_UP));
            dto.setProfitSharePercent(profitSharePercent);
            responses.add(dto);
        }

        return responses;
    }

    @Transactional
    public PortfolioPositionResponse create(PortfolioPositionRequest request) {
        securityService.assertTickerExists(request.getTicker());

        PortfolioPosition position = new PortfolioPosition();
        applyRequest(position, request);
        PortfolioPosition saved = repository.save(position);
        return enrichSingle(saved);
    }

    @Transactional
    public PortfolioPositionResponse update(Long id, PortfolioPositionRequest request) {
        PortfolioPosition existing = repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Portfolio position not found: " + id));
        securityService.assertTickerExists(request.getTicker());

        applyRequest(existing, request);
        PortfolioPosition saved = repository.save(existing);
        return enrichSingle(saved);
    }

    @Transactional
    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw new NotFoundException("Portfolio position not found: " + id);
        }
        repository.deleteById(id);
    }

    private PortfolioPositionResponse enrichSingle(PortfolioPosition position) {
        Map<String, BigDecimal> prices = priceService.getPricesUsd(Set.of(position.getTicker()));
        BigDecimal priceUsd = prices.getOrDefault(position.getTicker(), BigDecimal.ZERO);

        BigDecimal investedUsd = toUsd(position.getBuyPrice(), position.getCurrency())
                .multiply(position.getQuantity());
        BigDecimal targetUsd = Optional.ofNullable(position.getTargetPrice())
                .map(tp -> toUsd(tp, position.getCurrency()))
                .orElse(priceUsd);
        BigDecimal expectedUsd = targetUsd.multiply(position.getQuantity());
        BigDecimal expectedProfitUsd = expectedUsd.subtract(investedUsd);
        BigDecimal upsidePercent = computeUpside(priceUsd, targetUsd);

        PortfolioPositionResponse dto = mapToResponse(position);
        dto.setCurrentPriceUsd(priceUsd);
        dto.setUpsidePercent(upsidePercent);
        dto.setInvestedAmountUsd(investedUsd);
        dto.setSharePercent(BigDecimal.ZERO);
        dto.setExpectedProfitUsd(expectedProfitUsd);
        dto.setProfitSharePercent(BigDecimal.ZERO);
        return dto;
    }

    private void applyRequest(PortfolioPosition position, PortfolioPositionRequest request) {
        position.setSector(request.getSector());
        position.setCompany(request.getCompany());
        position.setTicker(request.getTicker());
        position.setBuyPrice(request.getBuyPrice());
        position.setTargetPrice(request.getTargetPrice());
        position.setQuantity(request.getQuantity());
        position.setCurrency(request.getCurrency().toUpperCase());
        position.setNotes(request.getNotes());
    }

    private PortfolioPositionResponse mapToResponse(PortfolioPosition position) {
        PortfolioPositionResponse dto = new PortfolioPositionResponse();
        dto.setId(position.getId());
        dto.setSector(position.getSector());
        dto.setCompany(position.getCompany());
        dto.setTicker(position.getTicker());
        dto.setBuyPrice(position.getBuyPrice());
        dto.setTargetPrice(position.getTargetPrice());
        dto.setQuantity(position.getQuantity());
        dto.setCurrency(position.getCurrency());
        dto.setNotes(position.getNotes());
        return dto;
    }

    private BigDecimal toUsd(BigDecimal amount, String currency) {
        if (amount == null) {
            return BigDecimal.ZERO;
        }
        if (currency == null) {
            return amount;
        }
        if ("EUR".equalsIgnoreCase(currency)) {
            return amount.multiply(fxProperties.getEurToUsd());
        }
        // Treat everything else as already USD for MVP.
        return amount;
    }

    private BigDecimal computeUpside(BigDecimal currentPriceUsd, BigDecimal targetUsd) {
        if (currentPriceUsd == null || targetUsd == null) {
            return BigDecimal.ZERO;
        }
        if (currentPriceUsd.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        return targetUsd.subtract(currentPriceUsd)
                .multiply(BigDecimal.valueOf(100))
                .divide(currentPriceUsd, 4, RoundingMode.HALF_UP);
    }
}

