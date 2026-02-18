package com.traderplanner.appbackend.controller;

import com.traderplanner.appbackend.dto.PortfolioPositionRequest;
import com.traderplanner.appbackend.dto.PortfolioPositionResponse;
import com.traderplanner.appbackend.service.PortfolioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/portfolio")
@Tag(name = "Portfolio")
public class PortfolioController {

	private final PortfolioService portfolioService;

	public PortfolioController(PortfolioService portfolioService) {
		this.portfolioService = portfolioService;
	}

	@GetMapping
	@Operation(summary = "List portfolio positions with computed analytics")
	public List<PortfolioPositionResponse> list() {
		return portfolioService.getAll();
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@Operation(summary = "Create portfolio position")
	public PortfolioPositionResponse create(@Valid @RequestBody PortfolioPositionRequest request) {
		return portfolioService.create(request);
	}

	@PutMapping("/{id}")
	@Operation(summary = "Update portfolio position")
	public PortfolioPositionResponse update(@PathVariable("id") Long id,
			@Valid @RequestBody PortfolioPositionRequest request) {
		return portfolioService.update(id, request);
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@Operation(summary = "Delete portfolio position")
	public void delete(@PathVariable("id") Long id) {
		portfolioService.delete(id);
	}
}
