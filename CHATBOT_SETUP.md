# Portfolio Intelligence Chatbot - Setup Guide

## Overview

The Portfolio Intelligence Chatbot is an enterprise-grade AI assistant that provides intelligent analysis of portfolio data, explains visualizations, and assists portfolio managers with insights and recommendations.

## Architecture

### Backend Components

1. **ChatbotController** (`/api/chatbot/chat`)
   - REST endpoint for chat interactions
   - No authentication required (as per requirements)

2. **ChatbotService**
   - Main orchestrator that coordinates all components
   - Combines RAG, LLM, Portfolio Analysis, and Visualization Explanation

3. **LLMService**
   - Gemini API integration with fallback strategy
   - Handles retries, rate limits, and multiple API keys
   - Automatic fallback on errors

4. **RAGService**
   - Retrieval-Augmented Generation
   - Loads knowledge from `/src/main/resources/knowledge/`
   - Provides context-aware knowledge retrieval

5. **PortfolioAnalysisService**
   - Analyzes portfolio data
   - Calculates metrics (Beta, VaR, concentration risk)
   - Generates risk warnings and alerts

6. **VisualizationExplanationService**
   - Strategy pattern for different chart types
   - Pluggable explanation logic
   - Supports: line, doughnut, bar, scatter charts

### Frontend Components

1. **Floating Chat Widget**
   - `Frontend/Css/chatbot.css` - Styles
   - `Frontend/JavaScriptCode/chatbot.js` - Widget logic
   - Integrated into `index.html`

2. **Visualization Integration**
   - Click handlers on charts
   - Automatic context capture
   - Metadata passing to chatbot

## Setup Instructions

### 1. Configure Gemini API Keys

Edit `src/main/resources/application.properties`:

```properties
# Add your Gemini API keys (comma-separated for fallback)
gemini.api-keys=YOUR_API_KEY_1,YOUR_API_KEY_2,YOUR_API_KEY_3
```

**To get Gemini API keys:**
1. Go to https://makersuite.google.com/app/apikey
2. Create API keys (create 2-3 for fallback)
3. Add them to `application.properties`

### 2. Build and Run Backend

```bash
cd "/Users/shubh/Desktop/Fortune/Fortune_Pro/Fortune"
./mvnw clean package
./mvnw spring-boot:run
```

The backend will start on `http://localhost:8080`

### 3. Start Frontend

Open `Frontend/Pages/index.html` in a browser, or use a simple HTTP server:

```bash
cd Frontend
python3 -m http.server 8000
# Then open http://localhost:8000/Pages/index.html
```

### 4. Test the Chatbot

1. Click the floating chat button (💬) in the bottom-right corner
2. Try asking:
   - "Explain this graph" (after clicking on a chart)
   - "What are the risks in my portfolio?"
   - "Which client portfolio needs attention?"
   - "Explain portfolio beta"

## Features

### ✅ Portfolio Analysis
- Fetches portfolio data from database
- Calculates Beta, VaR, concentration risk
- Identifies top/underperformers
- Generates risk warnings

### ✅ Visualization Intelligence
- Explains charts when clicked
- Context-aware explanations
- Supports multiple chart types (line, doughnut, bar, scatter)
- Extensible via Strategy pattern

### ✅ RAG Knowledge Base
- Financial definitions
- Risk heuristics
- Portfolio rules
- Visualization semantics
- Located in `src/main/resources/knowledge/`

### ✅ LLM Integration
- Gemini API with fallback
- Multiple API key support
- Automatic retry on failures
- Rate limit handling
- Confidence estimation

### ✅ Enterprise Standards
- Clear headings and bullet points
- Confidence indicators (LOW/MEDIUM/HIGH)
- No financial advice language
- Explainability > prediction
- Modular, extensible design

## API Endpoints

### POST `/api/chatbot/chat`

Request:
```json
{
  "message": "Explain this graph",
  "clientId": 1,
  "currentPage": "dashboard",
  "visualizationContext": {
    "chartType": "line",
    "chartId": "mainChart",
    "xAxis": "Time",
    "yAxis": "Portfolio Value"
  }
}
```

Response:
```json
{
  "response": "## Analysis\n\n...",
  "confidence": "HIGH",
  "type": "ANALYTICAL",
  "insights": ["..."],
  "explanation": "...",
  "suggestedQuestions": ["..."]
}
```

### GET `/api/chatbot/health`

Health check endpoint.

## Extending the Chatbot

### Adding New Chart Types

1. Create a new strategy class implementing `VisualizationExplanationStrategy`:

```java
@Component
public class ScatterChartStrategy implements VisualizationExplanationStrategy {
    @Override
    public String explain(VisualizationMetadata metadata, String portfolioContext) {
        // Your explanation logic
    }
    
    @Override
    public boolean supports(String chartType) {
        return "scatter".equalsIgnoreCase(chartType);
    }
}
```

2. Spring will automatically detect and register it.

### Adding Knowledge Base Content

1. Add files to `src/main/resources/knowledge/`
2. Update `RAGService.loadKnowledgeBase()` to load them
3. Update `RAGService.retrieveContext()` to use them

### Customizing LLM Prompts

Edit `LLMService.buildPrompt()` to customize the system prompt.

## Troubleshooting

### Chatbot shows "Service Temporarily Unavailable"
- Check if Gemini API keys are configured
- Verify API keys are valid
- Check backend logs for errors

### Charts not triggering chatbot
- Ensure `chatbot.js` is loaded
- Check browser console for errors
- Verify chart click handlers are attached

### No portfolio data in responses
- Ensure a client is selected in the dropdown
- Check database has portfolio data
- Verify `/api/portfolio/{clientId}` endpoint works

## Security Notes

- No authentication implemented (as per requirements)
- API keys stored in `application.properties` (use environment variables in production)
- All portfolio operations are read-only (no mutations)

## Future Enhancements

- Vector database for advanced RAG
- Conversation history persistence
- Multi-language support
- Voice input/output
- Advanced chart analysis with ML
