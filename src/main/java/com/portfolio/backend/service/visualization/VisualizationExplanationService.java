package com.portfolio.backend.service.visualization;

import com.portfolio.backend.dto.chatbot.VisualizationMetadata;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class VisualizationExplanationService implements VisualizationExplanationServiceInterface {
    
    private final List<VisualizationExplanationStrategy> strategies;
    
    public VisualizationExplanationService(List<VisualizationExplanationStrategy> strategies) {
        this.strategies = strategies;
    }
    
    public String explainVisualization(VisualizationMetadata metadata, String portfolioContext) {
        if (metadata == null || metadata.getChartType() == null) {
            return "## Visualization Context\n\nNo visualization context provided. Please hover over or select a chart to get specific insights.";
        }
        
        String chartType = metadata.getChartType();
        
        VisualizationExplanationStrategy strategy = strategies.stream()
                .filter(s -> s.supports(chartType))
                .findFirst()
                .orElse(new DefaultChartStrategy());
        
        return strategy.explain(metadata, portfolioContext);
    }
    
    private static class DefaultChartStrategy implements VisualizationExplanationStrategy {
        @Override
        public String explain(VisualizationMetadata metadata, String portfolioContext) {
            return "## Chart Analysis\n\nThis " + metadata.getChartType() + 
                   " chart displays portfolio data. Hover over specific data points for detailed insights.";
        }
        
        @Override
        public boolean supports(String chartType) {
            return true;
        }
    }
}
