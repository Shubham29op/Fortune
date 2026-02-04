package com.portfolio.backend.service.analysis;

import com.portfolio.backend.dto.chatbot.PortfolioAnalysisResult;

import java.math.BigDecimal;
import java.util.Map;

public interface PortfolioAnalysisServiceInterface {
    PortfolioAnalysisResult analyzePortfolio(Long clientId, Map<String, BigDecimal> currentPrices);
    String generatePortfolioSummary(PortfolioAnalysisResult analysis);
}
