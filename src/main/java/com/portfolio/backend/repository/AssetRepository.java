package com.portfolio.backend.repository;

import com.portfolio.backend.entity.Asset;
import com.portfolio.backend.entity.enums.AssetCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AssetRepository extends JpaRepository<Asset, Long> {
    
    // Custom query to find assets by category (e.g., "Give me all NSE stocks")
    List<Asset> findByCategory(AssetCategory category);

    // Custom query to find by symbol (e.g., "Find asset details for RELIANCE")
    Asset findBySymbol(String symbol);
}