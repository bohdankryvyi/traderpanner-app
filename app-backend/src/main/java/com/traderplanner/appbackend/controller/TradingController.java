package com.traderplanner.appbackend.controller;

import com.traderplanner.appbackend.dto.TradingEntryRequest;
import com.traderplanner.appbackend.dto.TradingEntryResponse;
import com.traderplanner.appbackend.service.TradingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/trading")
@Tag(name = "Trading")
public class TradingController {

	private final TradingService tradingService;

	public TradingController(TradingService tradingService) {
		this.tradingService = tradingService;
	}

	@GetMapping
	@Operation(summary = "List trading entries with current prices")
	public List<TradingEntryResponse> list() {
		return tradingService.getAll();
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@Operation(summary = "Create trading entry")
	public TradingEntryResponse create(@Valid @RequestBody TradingEntryRequest request) {
		return tradingService.create(request);
	}

	@PutMapping("/{id}")
	@Operation(summary = "Update trading entry")
	public TradingEntryResponse update(@PathVariable("id") Long id, @Valid @RequestBody TradingEntryRequest request) {
		return tradingService.update(id, request);
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@Operation(summary = "Delete trading entry")
	public void delete(@PathVariable("id") Long id) {
		tradingService.delete(id);
	}
}
