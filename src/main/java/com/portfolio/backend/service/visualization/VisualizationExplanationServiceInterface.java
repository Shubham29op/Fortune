package com.portfolio.backend.service.visualization;

import com.portfolio.backend.dto.chatbot.VisualizationMetadata;

public interface VisualizationExplanationServiceInterface {
    String explainVisualization(VisualizationMetadata metadata, String portfolioContext);
}
