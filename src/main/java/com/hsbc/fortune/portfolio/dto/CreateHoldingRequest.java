package com.hsbc.fortune.portfolio.dto;

import com.hsbc.fortune.portfolio.domain.Holding;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateHoldingRequest {
    @NotBlank(message = "Asset name is required")
    @Size(max = 200)
    private String assetName;

    @NotNull(message = "Category is required")
    private Holding.AssetCategory category;

    @NotNull(message = "Quantity is required")
    @DecimalMin(value = "0.0001", message = "Quantity must be greater than 0")
    private BigDecimal quantity;

    @NotNull(message = "Average price is required")
    @DecimalMin(value = "0.01", message = "Average price must be greater than 0")
    private BigDecimal avgPrice;

    @NotNull(message = "Current price is required")
    @DecimalMin(value = "0.01", message = "Current price must be greater than 0")
    private BigDecimal currentPrice;
}
