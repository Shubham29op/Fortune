package com.hsbc.fortune.portfolio.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DashboardSummaryDto {
    private BigDecimal totalAUM;
    private Integer activeClients;
    private BigDecimal avgReturns;
    private BigDecimal avgSharpeRatio;
    private Integer transactionsToday;
    private List<CustomerSummaryDto> topClients;
    private List<TransactionDto> recentTransactions;
    private Map<String, BigDecimal> assetAllocation;
    private List<PortfolioPerformanceDto> performanceData;
}
