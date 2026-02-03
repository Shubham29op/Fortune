package com.portfolio.backend.service;

import com.portfolio.backend.dto.BuyAssetRequest;
import com.portfolio.backend.entity.Asset;
import com.portfolio.backend.entity.Client;
import com.portfolio.backend.entity.ClientHolding;
import com.portfolio.backend.repository.AssetRepository;
import com.portfolio.backend.repository.ClientHoldingRepository;
import com.portfolio.backend.repository.ClientRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
public class PortfolioService implements PortfolioServiceInterface {

    @Autowired
    private ClientHoldingRepository holdingRepository;

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private AssetRepository assetRepository;

    @Transactional
    public ClientHolding buyAsset(BuyAssetRequest request) {
        // 1. Validate Client
        Client client = clientRepository.findById(request.getClientId())
                .orElseThrow(() -> new RuntimeException("Client not found"));

        // 2. Validate Asset
        Asset asset = assetRepository.findById(request.getAssetId())
                .orElseThrow(() -> new RuntimeException("Asset not found"));

        // 3. RULE CHECK: Max 5 Assets per Category
        long currentCount = holdingRepository.countByClientAndCategory(client.getClientId(), asset.getCategory());
        
        // Note: You can adjust the limit for Commodities (3) vs Stocks (5) here if you want
        int limit = asset.getCategory().name().equals("COMMODITY") ? 3 : 5;

        if (currentCount >= limit) {
            throw new RuntimeException("Limit Reached! You cannot hold more than " + limit + " " + asset.getCategory() + " assets.");
        }

        // 4. Create the Holding
        ClientHolding holding = new ClientHolding();
        holding.setClient(client);
        holding.setAsset(asset);
        holding.setQuantity(request.getQuantity());
        holding.setAvgBuyPrice(request.getPrice());
        holding.setBuyDate(LocalDate.now());

        return holdingRepository.save(holding);
    }
}