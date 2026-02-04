package com.portfolio.backend.service.visualization;

import com.portfolio.backend.dto.chatbot.VisualizationMetadata;
import org.springframework.stereotype.Component;

@Component
public class BarChartStrategy implements VisualizationExplanationStrategy {
    
    @Override
    public String explain(VisualizationMetadata metadata, String portfolioContext) {
        StringBuilder explanation = new StringBuilder();
        
        explanation.append("## Bar Chart Analysis\n\n");
        explanation.append("This bar chart compares **").append(metadata.getYAxis() != null ? metadata.getYAxis() : "values")
                  .append("** across **").append(metadata.getXAxis() != null ? metadata.getXAxis() : "categories").append("**.\n\n");
        
        explanation.append("**Interpretation:**\n");
        explanation.append("• **Taller bars**: Indicate higher values or greater exposure\n");
        explanation.append("• **Shorter bars**: Represent lower values or minimal exposure\n");
        explanation.append("• **Comparison**: Use to identify top performers or areas needing attention\n\n");
        
        if (metadata.getHoverData() != null && !metadata.getHoverData().isEmpty()) {
            explanation.append("**Selected Bar Details:**\n");
            metadata.getHoverData().forEach((key, value) -> {
                explanation.append("- ").append(key).append(": ").append(value).append("\n");
            });
        }
        
        return explanation.toString();
    }
    
    @Override
    public boolean supports(String chartType) {
        return "bar".equalsIgnoreCase(chartType);
    }
}
