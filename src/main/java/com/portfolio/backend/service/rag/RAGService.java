package com.portfolio.backend.service.rag;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

/**
 * RAG (Retrieval-Augmented Generation) Service
 * Retrieves relevant knowledge from knowledge base files
 */
@Service
@Slf4j
public class RAGService implements RAGServiceInterface {
    
    private static final String KNOWLEDGE_BASE_PATH = "knowledge/";
    private Map<String, String> knowledgeBase = new HashMap<>();
    
    public RAGService() {
        loadKnowledgeBase();
    }
    
 
    public String retrieveContext(String query, String currentPage, String chartType) {
        List<String> relevantSnippets = new ArrayList<>();
        String lowerQuery = query.toLowerCase();
        
        
        if (containsKeyword(lowerQuery, "risk", "var", "volatility", "beta")) {
            relevantSnippets.add(knowledgeBase.getOrDefault("risk_heuristics", ""));
        }
        
        if (containsKeyword(lowerQuery, "portfolio", "allocation", "diversification", "concentration")) {
            relevantSnippets.add(knowledgeBase.getOrDefault("portfolio_rules", ""));
        }
        
        if (containsKeyword(lowerQuery, "chart", "graph", "visualization", "trend") || chartType != null) {
            relevantSnippets.add(knowledgeBase.getOrDefault("visualization_semantics", ""));
            if (chartType != null) {
                relevantSnippets.add(getChartSpecificContext(chartType));
            }
        }
        
        if (containsKeyword(lowerQuery, "asset", "stock", "commodity", "mutual fund", "performance")) {
            relevantSnippets.add(knowledgeBase.getOrDefault("financial_definitions", ""));
        }
        
       
        relevantSnippets.add(knowledgeBase.getOrDefault("portfolio_rules", ""));
        
        return relevantSnippets.stream()
                .filter(s -> s != null && !s.isEmpty())
                .collect(Collectors.joining("\n\n"));
    }
    
    private boolean containsKeyword(String text, String... keywords) {
        return Arrays.stream(keywords).anyMatch(text::contains);
    }
    
    private String getChartSpecificContext(String chartType) {
        Map<String, String> chartContexts = Map.of(
            "line", "Line charts show trends over time. Look for patterns like upward trends (bullish), downward trends (bearish), or volatility spikes.",
            "doughnut", "Doughnut charts show allocation percentages. Large segments indicate concentration risk. Balanced portfolios show more evenly distributed segments.",
            "bar", "Bar charts compare values across categories. Higher bars indicate greater exposure or performance.",
            "scatter", "Scatter plots show correlation between two metrics. Points clustered together indicate similar risk/return profiles."
        );
        return chartContexts.getOrDefault(chartType.toLowerCase(), "");
    }
    
    private void loadKnowledgeBase() {
        try {
          
            knowledgeBase.put("financial_definitions", loadFile("financial_definitions.txt"));
            knowledgeBase.put("risk_heuristics", loadFile("risk_heuristics.txt"));
            knowledgeBase.put("portfolio_rules", loadFile("portfolio_rules.txt"));
            knowledgeBase.put("visualization_semantics", loadFile("visualization_semantics.txt"));
            
            log.info("Knowledge base loaded successfully");
        } catch (Exception e) {
            log.error("Error loading knowledge base", e);
       
            knowledgeBase.put("financial_definitions", getDefaultFinancialDefinitions());
            knowledgeBase.put("risk_heuristics", getDefaultRiskHeuristics());
            knowledgeBase.put("portfolio_rules", getDefaultPortfolioRules());
            knowledgeBase.put("visualization_semantics", getDefaultVisualizationSemantics());
        }
    }
    
    private String loadFile(String filename) {
        try {
            ClassPathResource resource = new ClassPathResource(KNOWLEDGE_BASE_PATH + filename);
            if (resource.exists()) {
                return new String(resource.getInputStream().readAllBytes());
            }
        } catch (IOException e) {
            log.warn("Could not load knowledge file: {}", filename);
        }
        return "";
    }
    
    
    private String getDefaultFinancialDefinitions() {
        return """
            FINANCIAL DEFINITIONS:
            - Portfolio: Collection of investments held by a client
            - Asset: Individual investment (stock, commodity, mutual fund)
            - Holding: Specific position in an asset
            - P&L (Profit & Loss): Difference between current value and cost basis
            - Unrealized P&L: Gains/losses not yet realized through sale
            - Realized P&L: Gains/losses from completed trades
            - Beta: Measure of portfolio volatility relative to market (1.0 = market average)
            - VaR (Value at Risk): Maximum potential loss at a given confidence level
            - Concentration Risk: Over-exposure to a single asset or category
            """;
    }
    
    private String getDefaultRiskHeuristics() {
        return """
            RISK HEURISTICS:
            - Beta > 1.2: Aggressive portfolio (high volatility)
            - Beta 0.8-1.2: Moderate portfolio
            - Beta < 0.8: Conservative portfolio
            - Commodity exposure > 30%: Higher volatility risk
            - Single asset > 20% of portfolio: Concentration risk
            - VaR at 95% confidence: 5% chance of loss exceeding calculated amount
            - High unrealized P&L volatility: Consider rebalancing
            """;
    }
    
    private String getDefaultPortfolioRules() {
        return """
            PORTFOLIO RULES:
            - Maximum 5 assets per category (NSE, MF)
            - Maximum 3 assets per category (Commodity)
            - Diversification reduces risk
            - Regular rebalancing maintains target allocation
            - Monitor concentration risk weekly
            - Review performance monthly
            """;
    }
    
    private String getDefaultVisualizationSemantics() {
        return """
            VISUALIZATION SEMANTICS:
            - Line charts: Show trends over time (performance, value changes)
            - Doughnut/Pie charts: Show allocation percentages (category distribution)
            - Bar charts: Compare values across categories
            - Scatter plots: Show correlation between metrics (risk vs return)
            - Upward trends: Positive performance
            - Downward trends: Negative performance or market correction
            - Sharp spikes/dips: Volatility events
            """;
    }
}
