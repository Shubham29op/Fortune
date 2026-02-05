package com.portfolio.backend.dto.chatbot;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.Map;

/**
 * Metadata for visualization context
 * Used when user interacts with charts/graphs
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class VisualizationMetadata {
    private String chartType; 
    private String chartId; 
    private String xAxis;
    private String yAxis; 
    private String assetSymbol; 
    private Long portfolioId; 
    private String timeRange;
    private Map<String, Object> calculatedMetrics; 
    private Map<String, Object> hoverData; 
}
