package com.portfolio.backend.service;

import com.portfolio.backend.dto.BuyAssetRequest;
import com.portfolio.backend.entity.Asset;
import com.portfolio.backend.entity.Client;
import com.portfolio.backend.entity.ClientHolding;
import com.portfolio.backend.exception.BusinessException;
import com.portfolio.backend.exception.ResourceNotFoundException;
import com.portfolio.backend.repository.AssetRepository;
import com.portfolio.backend.repository.ClientHoldingRepository;
import com.portfolio.backend.repository.ClientRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

@Service
public class PortfolioService {

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
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Client not found with id " + request.getClientId()
                        )
                );

        // 2. Validate Asset
        Asset asset = assetRepository.findById(request.getAssetId())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Asset not found with id " + request.getAssetId()
                        )
                );

        // 3. BUSINESS RULE: Max assets per category
        long currentCount = holdingRepository
                .countByClientAndCategory(client.getClientId(), asset.getCategory());

        int limit = asset.getCategory().name().equals("COMMODITY") ? 3 : 5;

        if (currentCount >= limit) {
            throw new BusinessException(
                    "Limit reached! You cannot hold more than "
                            + limit + " assets in category "
                            + asset.getCategory()
            );
        }

        // 4. Create Holding
        ClientHolding holding = new ClientHolding();
        holding.setClient(client);
        holding.setAsset(asset);

        // FIX #1: int → BigDecimal
        holding.setQuantity(BigDecimal.valueOf(request.getQuantity()));


        holding.setBuyDate(LocalDate.now());

        return holdingRepository.save(holding);
    }
}
