package com.portfolio.backend.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class BuyAssetRequest {

    @NotNull(message = "Client ID must not be null")
    private Long clientId;

    @NotNull(message = "Asset ID must not be null")
    private Long assetId;

    @Min(value = 1, message = "Quantity must be at least 1")
    private int quantity;

    public Long getClientId() {
        return clientId;
    }

    public Long getAssetId() {
        return assetId;
    }

    public int getQuantity() {
        return quantity;
    }
}
