package com.portfolio.backend.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class BuyAssetRequest {
    private Long clientId;
    private Long assetId;
    private BigDecimal quantity;
    private BigDecimal price; // The price at the moment of buying
}