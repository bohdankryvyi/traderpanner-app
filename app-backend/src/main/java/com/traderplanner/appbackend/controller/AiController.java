package com.traderplanner.appbackend.controller;

import com.traderplanner.appbackend.dto.AiTipsResponse;
import com.traderplanner.appbackend.dto.PatternResponse;
import com.traderplanner.appbackend.service.AiTipsService;
import com.traderplanner.appbackend.service.PatternService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai")
@Tag(name = "AI")
public class AiController {

    private final AiTipsService aiTipsService;
    private final PatternService patternService;

    public AiController(AiTipsService aiTipsService, PatternService patternService) {
        this.aiTipsService = aiTipsService;
        this.patternService = patternService;
    }

    @PostMapping("/tips")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Get AI trading tips", description = "timeframe param tf=1h|1d; uses cached tips for ai.cacheTtlMinutes")
    public AiTipsResponse getTips(@RequestParam("tf") String timeframe) {
        return aiTipsService.getTips(timeframe);
    }

    @PostMapping("/pattern")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Analyze now – single best ticker and pattern", description = "POST with tf=1h|1d. Returns ticker and technical pattern from OpenAI using real OHLC data.")
    public PatternResponse getPattern(@RequestParam("tf") String timeframe) {
        return patternService.getPattern(timeframe);
    }
}

