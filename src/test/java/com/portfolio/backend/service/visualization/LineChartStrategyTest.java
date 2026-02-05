package com.portfolio.backend.service.visualization;

import com.portfolio.backend.dto.chatbot.VisualizationMetadata;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class LineChartStrategyTest {

    private LineChartStrategy strategy;
    private VisualizationMetadata metadata;

    @BeforeEach
    void setUp() {
        strategy = new LineChartStrategy();
        metadata = new VisualizationMetadata();
    }

    @Test
    void testSupports_LineChart() {
        assertTrue(strategy.supports("line"));
        assertTrue(strategy.supports("LINE"));
        assertTrue(strategy.supports("Line"));
    }

    @Test
    void testSupports_NonLineChart() {
        assertFalse(strategy.supports("bar"));
        assertFalse(strategy.supports("doughnut"));
    }

    @Test
    void testExplain_WithHoverData() {
        metadata.setChartType("line");
        metadata.setXAxis("Time");
        metadata.setYAxis("Portfolio Value");
        Map<String, Object> hoverData = new HashMap<>();
        hoverData.put("Date", "2024-01-15");
        hoverData.put("Value", "$100000");
        metadata.setHoverData(hoverData);

        String explanation = strategy.explain(metadata, null);

        assertNotNull(explanation);
        assertTrue(explanation.contains("Line Chart Analysis"));
        assertTrue(explanation.contains("Time"));
        assertTrue(explanation.contains("Portfolio Value"));
        assertTrue(explanation.contains("2024-01-15"));
        assertTrue(explanation.contains("$100000"));
    }

    @Test
    void testExplain_WithPortfolioContext() {
        metadata.setChartType("line");
        String portfolioContext = "Portfolio beta: 1.2";

        String explanation = strategy.explain(metadata, portfolioContext);

        assertNotNull(explanation);
        assertTrue(explanation.contains("Portfolio Context"));
        assertTrue(explanation.contains("1.2"));
    }

    @Test
    void testExplain_WithNullFields() {
        metadata.setChartType("line");

        String explanation = strategy.explain(metadata, null);

        assertNotNull(explanation);
        assertTrue(explanation.contains("Line Chart Analysis"));
        assertTrue(explanation.contains("portfolio value"));
        assertTrue(explanation.contains("time"));
    }
}
