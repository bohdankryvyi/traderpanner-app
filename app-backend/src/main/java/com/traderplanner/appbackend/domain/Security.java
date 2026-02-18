package com.traderplanner.appbackend.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "securities")
public class Security {

	@Id
	@Column(name = "ticker", length = 32, nullable = false, updatable = false)
	private String ticker;

	@Column(name = "company", nullable = false, length = 255)
	private String company;

	@Column(name = "sector", nullable = false, length = 255)
	private String sector;

	protected Security() {
		// for JPA
	}

	public Security(String ticker, String company, String sector) {
		this.ticker = ticker;
		this.company = company;
		this.sector = sector;
	}

	public String getTicker() {
		return ticker;
	}

	public void setTicker(String ticker) {
		this.ticker = ticker;
	}

	public String getCompany() {
		return company;
	}

	public void setCompany(String company) {
		this.company = company;
	}

	public String getSector() {
		return sector;
	}

	public void setSector(String sector) {
		this.sector = sector;
	}
}
