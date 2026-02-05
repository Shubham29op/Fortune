package com.portfolio.backend.service.visualization;

import com.portfolio.backend.dto.chatbot.VisualizationMetadata;
import org.springframework.stereotype.Component;

@Component
public class DoughnutChartStrategy implements VisualizationExplanationStrategy {
    
    @Override
    public String explain(VisualizationMetadata metadata, String portfolioContext) {
        StringBuilder explanation = new StringBuilder();
        
        explanation.append("## Allocation Chart Analysis\n\n");
        explanation.append("This doughnut chart shows **portfolio allocation** across different asset categories.\n\n");
        
        if (metadata.getCalculatedMetrics() != null && !metadata.getCalculatedMetrics().isEmpty()) {
            explanation.append("**Current Allocation:**\n");
            metadata.getCalculatedMetrics().forEach((category, percentage) -> {
                explanation.append("• ").append(category).append(": ").append(percentage).append("%\n");
            });
            explanation.append("\n");
        }
        
        explanation.append("**Risk Assessment:**\n");
        explanation.append("• **Balanced allocation** (30-40% per category): Lower concentration risk\n");
        explanation.append("• **High concentration** (>50% in one category): Increased volatility risk\n");
        explanation.append("• **Commodity exposure** >30%: Higher volatility expected\n");
        explanation.append("• **Diversification**: Spreads risk across asset classes\n\n");
        
        explanation.append("**Recommendation:**\n");
        explanation.append("Monitor allocation weekly. Consider rebalancing if any category exceeds 50% of total portfolio value.\n");
        
        if (portfolioContext != null && !portfolioContext.isEmpty()) {
            explanation.append("\n**Portfolio Context:**\n").append(portfolioContext);
        }
        
        return explanation.toString();
    }
    
    @Override
    public boolean supports(String chartType) {
        return "doughnut".equalsIgnoreCase(chartType) || "pie".equalsIgnoreCase(chartType);
    }
}
