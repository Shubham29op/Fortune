package com.portfolio.backend.service.visualization;

import com.portfolio.backend.dto.chatbot.VisualizationMetadata;

/**
 * Strategy interface for visualization explanations
 * Allows pluggable explanation logic for different chart types
 */
public interface VisualizationExplanationStrategy {
    String explain(VisualizationMetadata metadata, String portfolioContext);
    boolean supports(String chartType);
}
