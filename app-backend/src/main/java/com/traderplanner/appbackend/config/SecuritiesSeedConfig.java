package com.traderplanner.appbackend.config;

import com.traderplanner.appbackend.service.SecurityService;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class SecuritiesSeedConfig {

    private final SecurityService securityService;

    public SecuritiesSeedConfig(SecurityService securityService) {
        this.securityService = securityService;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void seedSecurities() {
        securityService.seedFromCsvIfNeeded();
    }
}

