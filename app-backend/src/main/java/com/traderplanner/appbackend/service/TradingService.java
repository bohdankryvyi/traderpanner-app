package com.traderplanner.appbackend.service;

import com.traderplanner.appbackend.domain.TradingEntry;
import com.traderplanner.appbackend.dto.TradingEntryRequest;
import com.traderplanner.appbackend.dto.TradingEntryResponse;
import com.traderplanner.appbackend.exception.NotFoundException;
import com.traderplanner.appbackend.repository.TradingEntryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class TradingService {

	private final TradingEntryRepository repository;
	private final SecurityService securityService;
	private final PriceService priceService;

	public TradingService(TradingEntryRepository repository, SecurityService securityService,
			PriceService priceService) {
		this.repository = repository;
		this.securityService = securityService;
		this.priceService = priceService;
	}

	@Transactional(readOnly = true)
	public List<TradingEntryResponse> getAll() {
		List<TradingEntry> entries = repository.findAll();
		Set<String> tickers = entries.stream().map(TradingEntry::getTicker).collect(Collectors.toSet());
		var prices = priceService.getPricesUsd(tickers);

		List<TradingEntryResponse> responses = new ArrayList<>();
		for (TradingEntry entry : entries) {
			TradingEntryResponse dto = mapToResponse(entry);
			BigDecimal price = prices.getOrDefault(entry.getTicker(), BigDecimal.ZERO);
			dto.setCurrentPriceUsd(price);
			responses.add(dto);
		}
		return responses;
	}

	@Transactional
	public TradingEntryResponse create(TradingEntryRequest request) {
		securityService.assertTickerExists(request.getTicker());
		TradingEntry entry = new TradingEntry();
		applyRequest(entry, request);
		TradingEntry saved = repository.save(entry);
		return enrichSingle(saved);
	}

	@Transactional
	public TradingEntryResponse update(Long id, TradingEntryRequest request) {
		TradingEntry existing = repository.findById(id)
				.orElseThrow(() -> new NotFoundException("Trading entry not found: " + id));
		securityService.assertTickerExists(request.getTicker());
		applyRequest(existing, request);
		TradingEntry saved = repository.save(existing);
		return enrichSingle(saved);
	}

	@Transactional
	public void delete(Long id) {
		if (!repository.existsById(id)) {
			throw new NotFoundException("Trading entry not found: " + id);
		}
		repository.deleteById(id);
	}

	private TradingEntryResponse enrichSingle(TradingEntry entry) {
		var prices = priceService.getPricesUsd(Set.of(entry.getTicker()));
		BigDecimal price = prices.getOrDefault(entry.getTicker(), BigDecimal.ZERO);
		TradingEntryResponse dto = mapToResponse(entry);
		dto.setCurrentPriceUsd(price);
		return dto;
	}

	private void applyRequest(TradingEntry entry, TradingEntryRequest request) {
		entry.setTicker(request.getTicker());
		entry.setNote(request.getNote());
		entry.setEntryPrice(request.getEntryPrice());
	}

	private TradingEntryResponse mapToResponse(TradingEntry entry) {
		TradingEntryResponse dto = new TradingEntryResponse();
		dto.setId(entry.getId());
		dto.setTicker(entry.getTicker());
		dto.setNote(entry.getNote());
		dto.setEntryPrice(entry.getEntryPrice());
		return dto;
	}
}
