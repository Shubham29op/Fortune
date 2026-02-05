package com.portfolio.backend.repository;

import com.portfolio.backend.entity.ClientHolding;
import com.portfolio.backend.entity.enums.AssetCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ClientHoldingRepository extends JpaRepository<ClientHolding, Long> {

    // Find all holdings for a specific client
    List<ClientHolding> findByClient_ClientId(Long clientId);

    // CRITICAL: This is the method causing the Service error.
    // It counts how many assets of a specific category a client already owns.
    @Query("SELECT COUNT(ch) FROM ClientHolding ch WHERE ch.client.clientId = :clientId AND ch.asset.category = :category")
    long countByClientAndCategory(@Param("clientId") Long clientId, @Param("category") AssetCategory category);
}