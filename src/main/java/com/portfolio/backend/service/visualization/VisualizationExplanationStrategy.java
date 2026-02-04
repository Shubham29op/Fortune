package com.portfolio.backend.service.visualization;

import com.portfolio.backend.dto.chatbot.VisualizationMetadata;

public interface VisualizationExplanationStrategy {
    String explain(VisualizationMetadata metadata, String portfolioContext);
    boolean supports(String chartType);
}
