package com.traderplanner.appbackend.dto;

import java.math.BigDecimal;

public class PortfolioPositionResponse {

    private Long id;

    private String sector;

    private String company;

    private String ticker;

    private BigDecimal buyPrice;

    private BigDecimal targetPrice;

    private BigDecimal quantity;

    private String currency;

    private String notes;

    // Computed fields (in USD)
    private BigDecimal currentPriceUsd;
    private BigDecimal upsidePercent;
    private BigDecimal investedAmountUsd;
    private BigDecimal sharePercent;
    private BigDecimal expectedProfitUsd;
    private BigDecimal profitSharePercent;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public BigDecimal getCurrentPriceUsd() {
        return currentPriceUsd;
    }

    public void setCurrentPriceUsd(BigDecimal currentPriceUsd) {
        this.currentPriceUsd = currentPriceUsd;
    }

    public BigDecimal getUpsidePercent() {
        return upsidePercent;
    }

    public void setUpsidePercent(BigDecimal upsidePercent) {
        this.upsidePercent = upsidePercent;
    }

    public BigDecimal getInvestedAmountUsd() {
        return investedAmountUsd;
    }

    public void setInvestedAmountUsd(BigDecimal investedAmountUsd) {
        this.investedAmountUsd = investedAmountUsd;
    }

    public BigDecimal getSharePercent() {
        return sharePercent;
    }

    public void setSharePercent(BigDecimal sharePercent) {
        this.sharePercent = sharePercent;
    }

    public BigDecimal getExpectedProfitUsd() {
        return expectedProfitUsd;
    }

    public void setExpectedProfitUsd(BigDecimal expectedProfitUsd) {
        this.expectedProfitUsd = expectedProfitUsd;
    }

    public BigDecimal getProfitSharePercent() {
        return profitSharePercent;
    }

    public void setProfitSharePercent(BigDecimal profitSharePercent) {
        this.profitSharePercent = profitSharePercent;
    }
}

