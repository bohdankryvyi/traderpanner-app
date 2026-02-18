package com.traderplanner.appbackend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ai")
public class AiProperties {

	/**
	 * Whether AI pattern endpoint is enabled. When false, pattern still returns
	 * fallback.
	 */
	private boolean enabled = true;

	/**
	 * Cache TTL for AI tips and pattern cache, in minutes.
	 */
	private int cacheTtlMinutes = 30;

	/**
	 * Max number of actual OpenAI calls per day (UTC). Exceeding returns fallback.
	 */
	private int maxRequestsPerDay = 10;

	/**
	 * OpenAI model for chat completions (e.g. gpt-4o-mini).
	 */
	private String model = "gpt-4o-mini";

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public int getCacheTtlMinutes() {
		return cacheTtlMinutes;
	}

	public void setCacheTtlMinutes(int cacheTtlMinutes) {
		this.cacheTtlMinutes = cacheTtlMinutes;
	}

	public int getMaxRequestsPerDay() {
		return maxRequestsPerDay;
	}

	public void setMaxRequestsPerDay(int maxRequestsPerDay) {
		this.maxRequestsPerDay = maxRequestsPerDay;
	}

	public String getModel() {
		return model;
	}

	public void setModel(String model) {
		this.model = model;
	}
}
