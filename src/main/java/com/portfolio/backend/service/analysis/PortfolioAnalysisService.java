package com.portfolio.backend.service.analysis;

import com.portfolio.backend.dto.chatbot.PortfolioAnalysisResult;
import com.portfolio.backend.entity.ClientHolding;
import com.portfolio.backend.entity.enums.AssetCategory;
import com.portfolio.backend.repository.ClientHoldingRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class PortfolioAnalysisService implements PortfolioAnalysisServiceInterface {
    
    @Autowired
    private ClientHoldingRepository holdingRepository;
    
    private static final Map<AssetCategory, Double> CATEGORY_WEIGHTS = Map.of(
        AssetCategory.COMMODITY, 1.5,
        AssetCategory.NSE, 1.0,
        AssetCategory.MF, 0.7
    );
    
    public PortfolioAnalysisResult analyzePortfolio(Long clientId, Map<String, BigDecimal> currentPrices) {
        List<ClientHolding> holdings = holdingRepository.findByClient_ClientId(clientId);
        
        if (holdings.isEmpty()) {
            return createEmptyResult(clientId);
        }
        
        PortfolioAnalysisResult result = new PortfolioAnalysisResult();
        result.setClientId(clientId);
        
        BigDecimal totalInvested = BigDecimal.ZERO;
        BigDecimal totalValue = BigDecimal.ZERO;
        BigDecimal weightedBetaSum = BigDecimal.ZERO;
        Map<AssetCategory, BigDecimal> categoryValue = new HashMap<>();
        Map<AssetCategory, BigDecimal> categoryInvested = new HashMap<>();
        List<PortfolioAnalysisResult.AssetPerformance> performances = new ArrayList<>();
        
        for (ClientHolding holding : holdings) {
            BigDecimal invested = holding.getAvgBuyPrice().multiply(holding.getQuantity());
            BigDecimal currentPrice = currentPrices.getOrDefault(
                holding.getAsset().getSymbol(), 
                holding.getAvgBuyPrice()
            );
            BigDecimal marketValue = currentPrice.multiply(holding.getQuantity());
            BigDecimal pnl = marketValue.subtract(invested);
            BigDecimal pnlPercentage = invested.compareTo(BigDecimal.ZERO) > 0 
                ? pnl.divide(invested, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100))
                : BigDecimal.ZERO;
            
            totalInvested = totalInvested.add(invested);
            totalValue = totalValue.add(marketValue);
            
            AssetCategory category = holding.getAsset().getCategory();
            categoryValue.put(category, categoryValue.getOrDefault(category, BigDecimal.ZERO).add(marketValue));
            categoryInvested.put(category, categoryInvested.getOrDefault(category, BigDecimal.ZERO).add(invested));
            
            Double weight = CATEGORY_WEIGHTS.getOrDefault(category, 1.0);
            weightedBetaSum = weightedBetaSum.add(marketValue.multiply(BigDecimal.valueOf(weight)));
            
            PortfolioAnalysisResult.AssetPerformance perf = new PortfolioAnalysisResult.AssetPerformance();
            perf.setSymbol(holding.getAsset().getSymbol());
            perf.setAssetName(holding.getAsset().getAssetName());
            perf.setPnl(pnl);
            perf.setPnlPercentage(pnlPercentage.doubleValue());
            perf.setMarketValue(marketValue);
            performances.add(perf);
        }
        
        Double portfolioBeta = totalValue.compareTo(BigDecimal.ZERO) > 0
            ? weightedBetaSum.divide(totalValue, 4, RoundingMode.HALF_UP).doubleValue()
            : 1.0;
        
        BigDecimal unrealizedPnL = totalValue.subtract(totalInvested);
        
        result.setTotalValue(totalValue);
        result.setTotalInvested(totalInvested);
        result.setUnrealizedPnL(unrealizedPnL);
        result.setPortfolioBeta(portfolioBeta);
        
        // Category exposure
        Map<String, BigDecimal> categoryExposure = new HashMap<>();
        Map<String, BigDecimal> categoryPercentage = new HashMap<>();
        for (AssetCategory cat : AssetCategory.values()) {
            BigDecimal catValue = categoryValue.getOrDefault(cat, BigDecimal.ZERO);
            categoryExposure.put(cat.name(), catValue);
            if (totalValue.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal percentage = catValue.divide(totalValue, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
                categoryPercentage.put(cat.name(), percentage);
            }
        }
        result.setCategoryExposure(categoryExposure);
        result.setCategoryPercentage(categoryPercentage);
        
        List<PortfolioAnalysisResult.AssetPerformance> sortedPerf = performances.stream()
            .sorted((a, b) -> b.getPnlPercentage().compareTo(a.getPnlPercentage()))
            .collect(Collectors.toList());
        
        result.setTopPerformers(sortedPerf.stream().limit(3).collect(Collectors.toList()));
        result.setUnderPerformers(sortedPerf.stream()
            .sorted(Comparator.comparing(PortfolioAnalysisResult.AssetPerformance::getPnlPercentage))
            .limit(3)
            .collect(Collectors.toList()));
        
        result.setRiskWarnings(generateRiskWarnings(result));
        result.setConcentrationAlerts(generateConcentrationAlerts(result));
        
        return result;
    }
    
    private List<String> generateRiskWarnings(PortfolioAnalysisResult result) {
        List<String> warnings = new ArrayList<>();
        
        if (result.getPortfolioBeta() > 1.2) {
            warnings.add("Portfolio Beta > 1.2: Aggressive portfolio with high volatility risk");
        }
        
        BigDecimal commodityExposure = result.getCategoryPercentage().getOrDefault("COMMODITY", BigDecimal.ZERO);
        if (commodityExposure.compareTo(BigDecimal.valueOf(30)) > 0) {
            warnings.add("Commodity exposure > 30%: Higher volatility expected");
        }
        
        if (result.getUnrealizedPnL().compareTo(BigDecimal.ZERO) < 0) {
            BigDecimal lossPercentage = result.getTotalInvested().compareTo(BigDecimal.ZERO) > 0
                ? result.getUnrealizedPnL().abs()
                    .divide(result.getTotalInvested(), 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                : BigDecimal.ZERO;
            if (lossPercentage.compareTo(BigDecimal.valueOf(10)) > 0) {
                warnings.add("Unrealized loss > 10%: Consider reviewing positions");
            }
        }
        
        return warnings;
    }
    
    private List<String> generateConcentrationAlerts(PortfolioAnalysisResult result) {
        List<String> alerts = new ArrayList<>();
        
        for (Map.Entry<String, BigDecimal> entry : result.getCategoryPercentage().entrySet()) {
            if (entry.getValue().compareTo(BigDecimal.valueOf(50)) > 0) {
                alerts.add("High concentration in " + entry.getKey() + ": " + 
                          entry.getValue().setScale(1, RoundingMode.HALF_UP) + "% of portfolio");
            }
        }
        
        return alerts;
    }
    
    private PortfolioAnalysisResult createEmptyResult(Long clientId) {
        PortfolioAnalysisResult result = new PortfolioAnalysisResult();
        result.setClientId(clientId);
        result.setTotalValue(BigDecimal.ZERO);
        result.setTotalInvested(BigDecimal.ZERO);
        result.setUnrealizedPnL(BigDecimal.ZERO);
        result.setPortfolioBeta(1.0);
        result.setCategoryExposure(new HashMap<>());
        result.setCategoryPercentage(new HashMap<>());
        result.setTopPerformers(new ArrayList<>());
        result.setUnderPerformers(new ArrayList<>());
        result.setRiskWarnings(new ArrayList<>());
        result.setConcentrationAlerts(new ArrayList<>());
        return result;
    }
    
    public String generatePortfolioSummary(PortfolioAnalysisResult analysis) {
        StringBuilder summary = new StringBuilder();
        
        summary.append("## Portfolio Summary\n\n");
        summary.append("**Total Portfolio Value:** $").append(analysis.getTotalValue().setScale(2, RoundingMode.HALF_UP)).append("\n");
        summary.append("**Total Invested:** $").append(analysis.getTotalInvested().setScale(2, RoundingMode.HALF_UP)).append("\n");
        summary.append("**Unrealized P&L:** $").append(analysis.getUnrealizedPnL().setScale(2, RoundingMode.HALF_UP)).append("\n");
        summary.append("**Portfolio Beta:** ").append(String.format("%.2f", analysis.getPortfolioBeta())).append("\n\n");
        
        summary.append("**Allocation:**\n");
        analysis.getCategoryPercentage().forEach((category, percentage) -> {
            summary.append("• ").append(category).append(": ").append(percentage.setScale(1, RoundingMode.HALF_UP)).append("%\n");
        });
        
        if (!analysis.getRiskWarnings().isEmpty()) {
            summary.append("\n**Risk Warnings:**\n");
            analysis.getRiskWarnings().forEach(warning -> summary.append("• ").append(warning).append("\n"));
        }
        
        if (!analysis.getConcentrationAlerts().isEmpty()) {
            summary.append("\n**Concentration Alerts:**\n");
            analysis.getConcentrationAlerts().forEach(alert -> summary.append("• ").append(alert).append("\n"));
        }
        
        return summary.toString();
    }
}
