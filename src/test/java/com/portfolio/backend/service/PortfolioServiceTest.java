package com.portfolio.backend.service;

import com.portfolio.backend.dto.BuyAssetRequest;
import com.portfolio.backend.entity.Asset;
import com.portfolio.backend.entity.Client;
import com.portfolio.backend.entity.ClientHolding;
import com.portfolio.backend.entity.enums.AssetCategory;
import com.portfolio.backend.repository.AssetRepository;
import com.portfolio.backend.repository.ClientHoldingRepository;
import com.portfolio.backend.repository.ClientRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PortfolioServiceTest {

    private ClientHoldingRepository holdingRepository;
    private ClientRepository clientRepository;
    private AssetRepository assetRepository;
    private PortfolioService portfolioService;

    @BeforeEach
    void setUp() {
        holdingRepository = mock(ClientHoldingRepository.class);
        clientRepository = mock(ClientRepository.class);
        assetRepository = mock(AssetRepository.class);

        portfolioService = new PortfolioService();
        portfolioService.getClass();

        // Wire fields via reflection to avoid changing production code
        try {
            var f1 = PortfolioService.class.getDeclaredField("holdingRepository");
            f1.setAccessible(true);
            f1.set(portfolioService, holdingRepository);

            var f2 = PortfolioService.class.getDeclaredField("clientRepository");
            f2.setAccessible(true);
            f2.set(portfolioService, clientRepository);

            var f3 = PortfolioService.class.getDeclaredField("assetRepository");
            f3.setAccessible(true);
            f3.set(portfolioService, assetRepository);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    @Test
    void buyAsset_createsHolding_whenClientAndAssetExist() {
        Client client = new Client();
        client.setClientId(1L);

        Asset asset = new Asset();
        asset.setAssetId(10L);
        asset.setCategory(AssetCategory.NSE);

        BuyAssetRequest request = new BuyAssetRequest();
        request.setClientId(1L);
        request.setAssetId(10L);
        request.setQuantity(new BigDecimal("5"));
        request.setPrice(new BigDecimal("100.00"));

        when(clientRepository.findById(1L)).thenReturn(Optional.of(client));
        when(assetRepository.findById(10L)).thenReturn(Optional.of(asset));
        when(holdingRepository.countByClientAndCategory(1L, AssetCategory.NSE)).thenReturn(0L);
        when(holdingRepository.save(any(ClientHolding.class))).thenAnswer(inv -> inv.getArgument(0));

        ClientHolding holding = portfolioService.buyAsset(request);

        assertNotNull(holding);
        assertEquals(client, holding.getClient());
        assertEquals(asset, holding.getAsset());
        assertEquals(new BigDecimal("5"), holding.getQuantity());
        assertEquals(new BigDecimal("100.00"), holding.getAvgBuyPrice());
    }

    @Test
    void buyAsset_throwsWhenClientMissing() {
        BuyAssetRequest request = new BuyAssetRequest();
        request.setClientId(99L);

        when(clientRepository.findById(99L)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class, () -> portfolioService.buyAsset(request));
        assertTrue(ex.getMessage().contains("Client not found"));
    }

    @Test
    void buyAsset_enforcesCategoryLimit() {
        Client client = new Client();
        client.setClientId(1L);

        Asset asset = new Asset();
        asset.setAssetId(10L);
        asset.setCategory(AssetCategory.COMMODITY);

        BuyAssetRequest request = new BuyAssetRequest();
        request.setClientId(1L);
        request.setAssetId(10L);
        request.setQuantity(new BigDecimal("1"));
        request.setPrice(new BigDecimal("50.00"));

        when(clientRepository.findById(1L)).thenReturn(Optional.of(client));
        when(assetRepository.findById(10L)).thenReturn(Optional.of(asset));
        when(holdingRepository.countByClientAndCategory(1L, AssetCategory.COMMODITY)).thenReturn(3L);

        RuntimeException ex = assertThrows(RuntimeException.class, () -> portfolioService.buyAsset(request));
        assertTrue(ex.getMessage().contains("Limit Reached"));
    }
}

