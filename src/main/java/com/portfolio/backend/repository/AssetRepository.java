package com.portfolio.backend.repository;

import com.portfolio.backend.entity.Asset;
import com.portfolio.backend.entity.enums.AssetCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AssetRepository extends JpaRepository<Asset, Long> {
    List<Asset> findByCategory(AssetCategory category);
    Asset findBySymbol(String symbol);
}