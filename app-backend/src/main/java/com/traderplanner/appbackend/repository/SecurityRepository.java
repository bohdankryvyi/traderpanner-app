package com.traderplanner.appbackend.repository;

import com.traderplanner.appbackend.domain.Security;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SecurityRepository extends JpaRepository<Security, String> {

    List<Security> findByTickerContainingIgnoreCaseOrCompanyContainingIgnoreCase(String ticker, String company);

    boolean existsByTickerIgnoreCase(String ticker);
}

