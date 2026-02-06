package com.traderplanner.appbackend.repository;

import com.traderplanner.appbackend.domain.TradingEntry;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TradingEntryRepository extends JpaRepository<TradingEntry, Long> {
}

