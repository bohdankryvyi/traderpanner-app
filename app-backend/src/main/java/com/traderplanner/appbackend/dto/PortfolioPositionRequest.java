package com.traderplanner.appbackend.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public class PortfolioPositionRequest {

	@NotBlank
	@Size(max = 255)
	private String sector;

	@NotBlank
	@Size(max = 255)
	private String company;

	@NotBlank
	@Size(max = 32)
	private String ticker;

	@NotNull
	@DecimalMin(value = "0.0", inclusive = false)
	private BigDecimal buyPrice;

	@DecimalMin(value = "0.0", inclusive = false)
	private BigDecimal targetPrice;

	@NotNull
	@DecimalMin(value = "0.0", inclusive = false)
	private BigDecimal quantity;

	@NotBlank
	@Size(max = 3)
	private String currency;

	private String notes;

	public String getSector() {
		return sector;
	}

	public void setSector(String sector) {
		this.sector = sector;
	}

	public String getCompany() {
		return company;
	}

	public void setCompany(String company) {
		this.company = company;
	}

	public String getTicker() {
		return ticker;
	}

	public void setTicker(String ticker) {
		this.ticker = ticker;
	}

	public BigDecimal getBuyPrice() {
		return buyPrice;
	}

	public void setBuyPrice(BigDecimal buyPrice) {
		this.buyPrice = buyPrice;
	}

	public BigDecimal getTargetPrice() {
		return targetPrice;
	}

	public void setTargetPrice(BigDecimal targetPrice) {
		this.targetPrice = targetPrice;
	}

	public BigDecimal getQuantity() {
		return quantity;
	}

	public void setQuantity(BigDecimal quantity) {
		this.quantity = quantity;
	}

	public String getCurrency() {
		return currency;
	}

	public void setCurrency(String currency) {
		this.currency = currency;
	}

	public String getNotes() {
		return notes;
	}

	public void setNotes(String notes) {
		this.notes = notes;
	}
}
