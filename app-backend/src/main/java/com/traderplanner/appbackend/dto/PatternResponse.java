package com.traderplanner.appbackend.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;

@Schema(description = "Single best ticker and pattern from AI analysis")
public class PatternResponse {

    @Schema(example = "1h", allowableValues = {"1h", "1d"})
    private String timeframe;

    @Schema(example = "AAPL")
    private String ticker;

    @Schema(example = "Ascending Triangle")
    private String pattern;

    @Schema(description = "Short rationale referencing the candles")
    private String rationale;

    @Schema(description = "When the analysis was generated", type = "string", format = "date-time")
    private OffsetDateTime generatedAt;

    public PatternResponse() {
    }

    public PatternResponse(String timeframe, String ticker, String pattern, String rationale, OffsetDateTime generatedAt) {
        this.timeframe = timeframe;
        this.ticker = ticker;
        this.pattern = pattern;
        this.rationale = rationale;
        this.generatedAt = generatedAt;
    }

    public String getTimeframe() {
        return timeframe;
    }

    public void setTimeframe(String timeframe) {
        this.timeframe = timeframe;
    }

    public String getTicker() {
        return ticker;
    }

    public void setTicker(String ticker) {
        this.ticker = ticker;
    }

    public String getPattern() {
        return pattern;
    }

    public void setPattern(String pattern) {
        this.pattern = pattern;
    }

    public String getRationale() {
        return rationale;
    }

    public void setRationale(String rationale) {
        this.rationale = rationale;
    }

    public OffsetDateTime getGeneratedAt() {
        return generatedAt;
    }

    public void setGeneratedAt(OffsetDateTime generatedAt) {
        this.generatedAt = generatedAt;
    }
}
