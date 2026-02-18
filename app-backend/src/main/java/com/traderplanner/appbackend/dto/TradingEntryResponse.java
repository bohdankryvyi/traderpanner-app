package com.traderplanner.appbackend.dto;

import java.math.BigDecimal;

public class TradingEntryResponse {

	private Long id;

	private String ticker;

	private String note;

	private BigDecimal entryPrice;

	private BigDecimal currentPriceUsd;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getTicker() {
		return ticker;
	}

	public void setTicker(String ticker) {
		this.ticker = ticker;
	}

	public String getNote() {
		return note;
	}

	public void setNote(String note) {
		this.note = note;
	}

	public BigDecimal getEntryPrice() {
		return entryPrice;
	}

	public void setEntryPrice(BigDecimal entryPrice) {
		this.entryPrice = entryPrice;
	}

	public BigDecimal getCurrentPriceUsd() {
		return currentPriceUsd;
	}

	public void setCurrentPriceUsd(BigDecimal currentPriceUsd) {
		this.currentPriceUsd = currentPriceUsd;
	}
}
