package com.portfolio.backend.service.chatbot;

import com.portfolio.backend.dto.chatbot.*;
import com.portfolio.backend.service.analysis.PortfolioAnalysisServiceInterface;
import com.portfolio.backend.service.llm.LLMServiceInterface;
import com.portfolio.backend.service.rag.RAGServiceInterface;
import com.portfolio.backend.service.visualization.VisualizationExplanationServiceInterface;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * Main orchestrator service for chatbot functionality
 * Coordinates RAG, LLM, Portfolio Analysis, and Visualization Explanation
 */
@Service
@Slf4j
public class ChatbotService implements ChatbotServiceInterface {

    @Autowired
    private LLMServiceInterface llmService;

    @Autowired
    private RAGServiceInterface ragService;

    @Autowired
    private PortfolioAnalysisServiceInterface portfolioAnalysisService;

    @Autowired
    private VisualizationExplanationServiceInterface visualizationExplanationService;
    
    /**
     * Process chat request and generate intelligent response
     */
    public ChatResponse processChat(ChatRequest request) {
        try {
            // 1. Build context from multiple sources
            String context = buildContext(request);
            
            // 2. Retrieve relevant knowledge (RAG)
            String ragContext = ragService.retrieveContext(
                request.getMessage(),
                request.getCurrentPage(),
                request.getVisualizationContext() != null ? request.getVisualizationContext().getChartType() : null
            );
            
            // 3. Handle visualization-specific queries
            if (request.getVisualizationContext() != null) {
                String vizExplanation = visualizationExplanationService.explainVisualization(
                    request.getVisualizationContext(),
                    context
                );
                context = vizExplanation + "\n\n" + context;
            }
            
            // 4. Combine all context
            String fullContext = ragContext + "\n\n" + context;
            
            // 5. Generate LLM response
            ChatResponse response = llmService.generateResponse(request.getMessage(), fullContext);
            
            // 6. Enhance response with portfolio insights if applicable
            if (request.getClientId() != null) {
                enhanceWithPortfolioInsights(response, request.getClientId());
            }
            
            // 7. Add suggested questions
            response.setSuggestedQuestions(generateSuggestedQuestions(request));
            
            return response;
            
        } catch (Exception e) {
            log.error("Error processing chat request", e);
            return createErrorResponse("I encountered an error processing your request. Please try again.");
        }
    }
    
    /**
     * Build context from portfolio data
     */
    private String buildContext(ChatRequest request) {
        StringBuilder context = new StringBuilder();
        
        if (request.getClientId() != null) {
            try {
                Map<String, BigDecimal> currentPrices = getCurrentPrices(request.getClientId());
                PortfolioAnalysisResult analysis = portfolioAnalysisService.analyzePortfolio(
                    request.getClientId(), 
                    currentPrices
                );
                String summary = portfolioAnalysisService.generatePortfolioSummary(analysis);
                context.append("--- BEGIN PORTFOLIO DATA (use this to answer) ---\n");
                context.append(summary);
                context.append("--- END PORTFOLIO DATA ---\n\n");
                log.debug("Built portfolio context for clientId={}, summary length={}", request.getClientId(), summary.length());
            } catch (Exception e) {
                log.warn("Failed to build portfolio context for clientId={}", request.getClientId(), e);
                context.append("(Portfolio data temporarily unavailable for this client.)\n\n");
            }
        } else {
            context.append("(No client selected in dropdown - suggest user to select an account for portfolio-specific answers.)\n\n");
        }
        
        if (request.getCurrentPage() != null) {
            context.append("**Current Page:** ").append(request.getCurrentPage()).append("\n");
        }
        
        return context.toString();
    }
    
    /**
     * Get current prices for assets (mock implementation)
     * In production, this would fetch from market data API
     */
    private Map<String, BigDecimal> getCurrentPrices(Long clientId) {
        // TODO: Integrate with actual market data service
        // For now, return empty map - PortfolioAnalysisService will use buy prices as fallback
        return new HashMap<>();
    }
    
    /**
     * Enhance response with portfolio-specific insights
     */
    private void enhanceWithPortfolioInsights(ChatResponse response, Long clientId) {
        // Add portfolio context to insights if not already present
        if (response.getInsights() == null || response.getInsights().isEmpty()) {
            Map<String, BigDecimal> currentPrices = getCurrentPrices(clientId);
            PortfolioAnalysisResult analysis = portfolioAnalysisService.analyzePortfolio(clientId, currentPrices);
            
            if (!analysis.getRiskWarnings().isEmpty()) {
                response.setInsights(analysis.getRiskWarnings());
            }
        }
    }
    
    /**
     * Generate contextual suggested questions
     */
    private java.util.List<String> generateSuggestedQuestions(ChatRequest request) {
        java.util.List<String> questions = new java.util.ArrayList<>();
        
        if (request.getVisualizationContext() != null) {
            questions.add("What does this chart tell me about my portfolio?");
            questions.add("Are there any anomalies in this visualization?");
        }
        
        if (request.getClientId() != null) {
            questions.add("What are the main risks in this portfolio?");
            questions.add("Which assets are performing best?");
            questions.add("Should I rebalance this portfolio?");
        }
        
        questions.add("Explain portfolio beta");
        questions.add("What is concentration risk?");
        
        return questions;
    }
    
    private ChatResponse createErrorResponse(String message) {
        ChatResponse response = new ChatResponse();
        response.setResponse("## Error\n\n" + message);
        response.setConfidence(ChatResponse.ConfidenceLevel.LOW);
        response.setType(ChatResponse.ResponseType.INFORMATIONAL);
        response.setExplanation("Error response");
        return response;
    }
}
