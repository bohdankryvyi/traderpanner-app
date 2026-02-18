package com.traderplanner.appbackend.domain;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "portfolio_positions")
public class PortfolioPosition {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id")
	private Long id;

	@Column(name = "sector", nullable = false, length = 255)
	private String sector;

	@Column(name = "company", nullable = false, length = 255)
	private String company;

	@Column(name = "ticker", nullable = false, length = 32)
	private String ticker;

	@Column(name = "buy_price", nullable = false, precision = 18, scale = 6)
	private BigDecimal buyPrice;

	@Column(name = "target_price", precision = 18, scale = 6)
	private BigDecimal targetPrice;

	@Column(name = "quantity", nullable = false, precision = 18, scale = 6)
	private BigDecimal quantity;

	@Column(name = "currency", nullable = false, length = 3)
	private String currency;

	@Column(name = "notes")
	private String notes;

	@Column(name = "created_at", nullable = false)
	private OffsetDateTime createdAt;

	@PrePersist
	public void prePersist() {
		if (createdAt == null) {
			createdAt = OffsetDateTime.now();
		}
	}

	public Long getId() {
		return id;
	}

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

	public OffsetDateTime getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(OffsetDateTime createdAt) {
		this.createdAt = createdAt;
	}
}
