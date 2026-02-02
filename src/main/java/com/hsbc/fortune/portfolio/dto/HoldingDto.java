package com.hsbc.fortune.portfolio.dto;

import com.hsbc.fortune.portfolio.domain.Holding;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class HoldingDto {
    private Long id;
    private String assetName;
    private Holding.AssetCategory category;
    private BigDecimal quantity;
    private BigDecimal avgPrice;
    private BigDecimal currentPrice;
    private BigDecimal investedAmount;
    private BigDecimal currentValue;
    private BigDecimal gain;
    private BigDecimal returns;
}
