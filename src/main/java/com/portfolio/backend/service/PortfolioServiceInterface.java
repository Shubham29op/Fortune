package com.portfolio.backend.service;

import com.portfolio.backend.dto.BuyAssetRequest;
import com.portfolio.backend.entity.ClientHolding;

public interface PortfolioServiceInterface {
    ClientHolding buyAsset(BuyAssetRequest request);
}
