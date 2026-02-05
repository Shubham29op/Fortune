package com.portfolio.backend.dto.chatbot;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.math.BigDecimal;
import java.util.Map;
import java.util.List;

/**
 * Portfolio analysis results for chatbot context
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PortfolioAnalysisResult {
    private Long clientId;
    private BigDecimal totalValue;
    private BigDecimal totalInvested;
    private BigDecimal unrealizedPnL;
    private BigDecimal realizedPnL;
    private Double portfolioBeta;
    private Map<String, BigDecimal> categoryExposure; // Category -> Value
    private Map<String, BigDecimal> categoryPercentage; // Category -> Percentage
    private List<AssetPerformance> topPerformers;
    private List<AssetPerformance> underPerformers;
    private List<String> riskWarnings;
    private List<String> concentrationAlerts;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AssetPerformance {
        private String symbol;
        private String assetName;
        private BigDecimal pnl;
        private Double pnlPercentage;
        private BigDecimal marketValue;
    }
}
