package com.portfolio.backend.service.visualization;

import com.portfolio.backend.dto.chatbot.VisualizationMetadata;
import org.springframework.stereotype.Component;

/**
 * Strategy for explaining line charts (performance trends)
 */
@Component
public class LineChartStrategy implements VisualizationExplanationStrategy {
    
    @Override
    public String explain(VisualizationMetadata metadata, String portfolioContext) {
        StringBuilder explanation = new StringBuilder();
        
        explanation.append("## Line Chart Analysis\n\n");
        explanation.append("This line chart displays **").append(metadata.getYAxis() != null ? metadata.getYAxis() : "portfolio value")
                  .append("** over **").append(metadata.getXAxis() != null ? metadata.getXAxis() : "time").append("**.\n\n");
        
        if (metadata.getHoverData() != null && !metadata.getHoverData().isEmpty()) {
            explanation.append("**At the selected point:**\n");
            metadata.getHoverData().forEach((key, value) -> {
                explanation.append("- ").append(key).append(": ").append(value).append("\n");
            });
            explanation.append("\n");
        }
        
        explanation.append("**What to look for:**\n");
        explanation.append("• **Upward trends**: Indicate positive performance and growth\n");
        explanation.append("• **Downward trends**: May signal market corrections or underperformance\n");
        explanation.append("• **Volatility spikes**: Sharp increases/decreases suggest high-risk periods\n");
        explanation.append("• **Steady growth**: Consistent upward movement indicates stable performance\n\n");
        
        if (portfolioContext != null && !portfolioContext.isEmpty()) {
            explanation.append("**Portfolio Context:**\n").append(portfolioContext).append("\n");
        }
        
        return explanation.toString();
    }
    
    @Override
    public boolean supports(String chartType) {
        return "line".equalsIgnoreCase(chartType);
    }
}
