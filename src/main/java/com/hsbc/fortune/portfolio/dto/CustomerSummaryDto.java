package com.hsbc.fortune.portfolio.dto;

import com.hsbc.fortune.portfolio.domain.Customer;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CustomerSummaryDto {
    private Long id;
    private String clientId;
    private String name;
    private Customer.ClientType type;
    private Customer.RiskLevel riskLevel;
    private LocalDateTime joinDate;
    private BigDecimal portfolioValue;
    private BigDecimal investedAmount;
    private BigDecimal totalGain;
    private BigDecimal totalReturns;
    private BigDecimal sharpeRatio;
    private Integer assetCount;
}
