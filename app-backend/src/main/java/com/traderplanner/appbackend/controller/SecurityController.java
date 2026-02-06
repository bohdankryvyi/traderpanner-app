package com.traderplanner.appbackend.controller;

import com.traderplanner.appbackend.dto.SecurityDto;
import com.traderplanner.appbackend.service.SecurityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/securities")
@Tag(name = "Securities")
public class SecurityController {

    private final SecurityService securityService;

    public SecurityController(SecurityService securityService) {
        this.securityService = securityService;
    }

    @GetMapping
    @Operation(summary = "List securities", description = "Optionally filter by ticker or company using q parameter")
    public List<SecurityDto> list(@RequestParam(name = "q", required = false) String query) {
        return securityService.findSecurities(query);
    }
}

