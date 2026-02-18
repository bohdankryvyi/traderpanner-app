package com.traderplanner.appbackend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;

@ConfigurationProperties(prefix = "fx")
public class FxProperties {

	/**
	 * Fixed EUR -> USD rate, e.g. 1.10.
	 */
	private BigDecimal eurToUsd;

	public BigDecimal getEurToUsd() {
		return eurToUsd;
	}

	public void setEurToUsd(BigDecimal eurToUsd) {
		this.eurToUsd = eurToUsd;
	}
}
