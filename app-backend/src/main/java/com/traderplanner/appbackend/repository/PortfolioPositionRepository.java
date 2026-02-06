package com.traderplanner.appbackend.repository;

import com.traderplanner.appbackend.domain.PortfolioPosition;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PortfolioPositionRepository extends JpaRepository<PortfolioPosition, Long> {
}

