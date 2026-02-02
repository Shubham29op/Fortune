package com.hsbc.fortune.portfolio.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PortfolioDto {
    private Long customerId;
    private String customerName;
    private String clientId;
    private BigDecimal totalInvested;
    private BigDecimal totalCurrentValue;
    private BigDecimal totalGain;
    private BigDecimal totalReturns;
    private BigDecimal sharpeRatio;
    private List<HoldingDto> holdings = new ArrayList<>();
}
