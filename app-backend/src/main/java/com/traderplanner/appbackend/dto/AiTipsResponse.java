package com.traderplanner.appbackend.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;

public class AiTipsResponse {

    private String timeframe;
    private String tipsText;

    @Schema(description = "When the tips were generated", type = "string", format = "date-time", example = "2025-01-01T12:00:00Z")
    private OffsetDateTime generatedAt;

    public AiTipsResponse() {
    }

    public AiTipsResponse(String timeframe, String tipsText, OffsetDateTime generatedAt) {
        this.timeframe = timeframe;
        this.tipsText = tipsText;
        this.generatedAt = generatedAt;
    }

    public String getTimeframe() {
        return timeframe;
    }

    public void setTimeframe(String timeframe) {
        this.timeframe = timeframe;
    }

    public String getTipsText() {
        return tipsText;
    }

    public void setTipsText(String tipsText) {
        this.tipsText = tipsText;
    }

    public OffsetDateTime getGeneratedAt() {
        return generatedAt;
    }

    public void setGeneratedAt(OffsetDateTime generatedAt) {
        this.generatedAt = generatedAt;
    }
}

