package com.traderplanner.appbackend.service;

import com.traderplanner.appbackend.domain.Security;
import com.traderplanner.appbackend.dto.SecurityDto;
import com.traderplanner.appbackend.repository.SecurityRepository;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Service
public class SecurityService {

	private final SecurityRepository securityRepository;

	public SecurityService(SecurityRepository securityRepository) {
		this.securityRepository = securityRepository;
	}

	@Transactional
	public void seedFromCsvIfNeeded() {
		try {
			ClassPathResource resource = new ClassPathResource("sp500.csv");
			if (!resource.exists()) {
				return;
			}

			try (BufferedReader reader = new BufferedReader(
					new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
				String line;
				boolean first = true;
				while ((line = reader.readLine()) != null) {
					if (first) {
						first = false; // skip header
						continue;
					}
					String[] parts = line.split(",", 3);
					if (parts.length < 3) {
						continue;
					}
					String ticker = parts[0].trim();
					String company = parts[1].trim();
					String sector = parts[2].trim();
					if (ticker.isEmpty()) {
						continue;
					}

					Security existing = securityRepository.findById(ticker).orElse(null);
					if (existing == null) {
						securityRepository.save(new Security(ticker, company, sector));
					} else {
						existing.setCompany(company);
						existing.setSector(sector);
						securityRepository.save(existing);
					}
				}
			}
		} catch (IOException e) {
			// For MVP just log and continue; seeding is best-effort.
			// A real implementation would use a logger.
			System.err.println("Failed to seed securities from CSV: " + e.getMessage());
		}
	}

	@Transactional(readOnly = true)
	public List<SecurityDto> findSecurities(String query) {
		List<Security> securities;
		if (query == null || query.isBlank()) {
			securities = securityRepository.findAll();
		} else {
			securities = securityRepository.findByTickerContainingIgnoreCaseOrCompanyContainingIgnoreCase(query, query);
		}
		List<SecurityDto> result = new ArrayList<>();
		for (Security security : securities) {
			result.add(new SecurityDto(security.getTicker(), security.getCompany(), security.getSector()));
		}
		return result;
	}

	@Transactional(readOnly = true)
	public void assertTickerExists(String ticker) {
		if (!securityRepository.existsByTickerIgnoreCase(ticker)) {
			throw new com.traderplanner.appbackend.exception.BadRequestException("Unknown ticker: " + ticker);
		}
	}
}
