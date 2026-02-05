# Portfolio Intelligence Chatbot – Full Documentation

This document describes the complete design and implementation of the chatbot module in the Fortune Pro portfolio management application.

---

## 1. Overview

The chatbot is an **enterprise-grade AI assistant** that:

- **Analyzes portfolio data** (read-only) using the existing backend and database.
- **Explains visualizations** (charts) using structured metadata, not images.
- **Uses RAG** (Retrieval-Augmented Generation) with a local knowledge base.
- **Calls a single LLM** (Gemini) with retries and multiple API keys for fallback.
- **Exposes a floating widget** on the frontend; all LLM calls go through the backend (no direct UI → LLM).

**Constraints:** No user authentication, no portfolio mutation, no direct API calls from UI to LLM, no vendor lock-in in design.

---

## 2. High-Level Architecture
graph LR
    subgraph Client_Interface [Frontend Layer]
        User((User))
        Widget[Interactive Chatbot UI]
    end

    subgraph Security_Gateway [Orchestration Layer]
        API[Spring Boot Controller]
        Auth{Auth & Context}
    end

    subgraph AI_Engine [Intelligence Engine]
        Service[Chatbot Service]
        LLM[Gemini Pro / LLM]
        Vector[(Vector DB / RAG)]
    end

    subgraph Enterprise_Data [Data Layer]
        SQL[(Portfolio DB)]
        KB[Knowledge Base]
    end

    %% Flow
    User -->|Queries| Widget
    Widget -->|Secure Request| API
    API --> Auth
    Auth --> Service
    
    Service -->|1. Context Retrieval| SQL
    Service -->|2. Semantic Search| Vector
    Vector --- KB
    
    Service -->|3. Augmented Prompt| LLM
    LLM -->|4. Insightful Response| Service
    Service -->|5. Structured Output| Widget
    Widget -->|Visualization| User

    %% Styling
    style LLM fill:#4285F4,stroke:#fff,color:#fff
    style Vector fill:#34A853,stroke:#fff,color:#fff
    style API fill:#FBBC05,stroke:#fff
    style User fill:#EA4335,stroke:#fff,color:#fff

## 3. Backend Components

### 3.1 Controller

**File:** `src/main/java/com/portfolio/backend/controller/ChatbotController.java`

- **Base path:** `/api/chatbot`
- **CORS:** `@CrossOrigin(origins = "*")` (no auth as per requirements)

| Method | Path        | Purpose |
|--------|-------------|---------|
| POST   | `/chat`     | Main chat endpoint. Body: `ChatRequest`. Returns `ChatResponse`. |
| GET    | `/health`   | Health check. Returns `{ "status": "healthy", "service": "chatbot" }`. |

**Behaviour:**

- Validates that `message` is non-null and non-empty.
- On success: returns `ChatbotService.processChat(request)`.
- On validation failure: 400 with a short error message in `ChatResponse`.
- On any other exception: 500 with a generic error message in `ChatResponse`.

---

### 3.2 DTOs (Data Transfer Objects)

**Package:** `com.portfolio.backend.dto.chatbot`

| DTO | Role |
|-----|------|
| **ChatRequest** | `message`, `clientId` (Long), `currentPage` (String), `visualizationContext` (VisualizationMetadata). |
| **ChatResponse** | `response` (markdown text), `confidence` (LOW/MEDIUM/HIGH), `type` (INFORMATIONAL/ANALYTICAL/EXPERIMENTAL), `insights`, `explanation`, `suggestedQuestions`. |
| **VisualizationMetadata** | `chartType`, `chartId`, `xAxis`, `yAxis`, `assetSymbol`, `portfolioId`, `timeRange`, `calculatedMetrics`, `hoverData` (maps for flexible payload). |
| **PortfolioAnalysisResult** | Used internally for analysis: clientId, totals, beta, category exposure, top/underperformers, risk/concentration alerts. |

All chatbot API request/response shapes are defined here; the frontend and controller use these contracts.

---

### 3.3 ChatbotService (Orchestrator)

**File:** `src/main/java/com/portfolio/backend/service/chatbot/ChatbotService.java`

**Entry point:** `processChat(ChatRequest request)` returns `ChatResponse`.

**Steps:**

1. **Build context**  
   `buildContext(request)`:
   - If `request.getClientId() != null`: loads portfolio via `PortfolioAnalysisService.analyzePortfolio(...)` and `generatePortfolioSummary(...)`.
   - Wraps portfolio text in `--- BEGIN PORTFOLIO DATA ---` / `--- END PORTFOLIO DATA ---`.
   - If no client selected, appends a line asking the user to select an account.
   - Appends current page if present.

2. **RAG**  
   `RAGService.retrieveContext(message, currentPage, chartType)` returns knowledge snippets (definitions, risk rules, viz semantics) that are concatenated into the prompt.

3. **Visualization**  
   If `request.getVisualizationContext() != null`, calls `VisualizationExplanationService.explainVisualization(metadata, context)` and prepends that explanation to the context.

4. **LLM**  
   `LLMService.generateResponse(request.getMessage(), fullContext)` where `fullContext = ragContext + context` (including portfolio and viz when applicable).

5. **Enhance**  
   If `clientId` is set and the response has no insights, fills `response.insights` from portfolio risk warnings.

6. **Suggested questions**  
   `generateSuggestedQuestions(request)` adds contextual follow-ups (e.g. “What are the main risks in this portfolio?” when a client is selected).

**Helper methods:**

- `getCurrentPrices(clientId)` – currently returns an empty map; placeholder for a future market-data integration. Portfolio analysis uses buy price as fallback when no current price is available.

---

### 3.4 LLMService (Gemini)

**File:** `src/main/java/com/portfolio/backend/service/llm/LLMService.java`

**Responsibility:** Call Gemini API with a single prompt; retry and fallback across API keys; parse response into `ChatResponse`.

**Configuration:** Injected `GeminiConfig`: base URL (model endpoint), list of API keys, max retries, timeout.

**Main method:** `generateResponse(String prompt, String context)`:

- Builds full prompt via `buildPrompt(userMessage, context)`.
- For each retry (up to `maxRetries`): picks next API key with `getNextApiKey()`, calls `callGeminiAPI(fullPrompt, apiKey)`.
- On failure: if error is retryable (e.g. 429, 5xx, timeout), waits with backoff and retries; otherwise returns `createFallbackResponse(prompt)`.

**Prompt rules (buildPrompt):**

- System instructions: portfolio analyst, crisp/short answers, use PORTFOLIO DATA when provided, do not say “without portfolio data” when data is present, no financial advice.
- Then: “CONTEXT / PORTFOLIO DATA:” + context.
- Then: “USER QUESTION:” + user message.
- Asks for concise reply (e.g. under 150 words, 3–5 bullets).

**callGeminiAPI:**

- Builds JSON body: `contents[].parts[].text` = full prompt; `safetySettings` for harm categories.
- POST to `config.getBaseUrl() + "?key=" + apiKey` using `WebClient`.
- Uses `Retry.backoff` for retryable errors and timeout.
- Parses JSON response: `candidates[0].content.parts[0].text` → response text.
- `parseGeminiResponse` maps that into `ChatResponse` (response text, confidence heuristic, type).

**Fallback:** If all calls fail or parsing fails, returns a “Service Temporarily Unavailable” `ChatResponse` with LOW confidence.

---

### 3.5 RAGService

**File:** `src/main/java/com/portfolio/backend/service/rag/RAGService.java`

**Role:** Retrieve relevant text from a **local knowledge base** (no vector DB). Used to augment the LLM prompt so answers stay aligned with rules and definitions.

**Knowledge base location:** `src/main/resources/knowledge/`

| File | Content |
|------|---------|
| `financial_definitions.txt` | Terms: portfolio, P&L, beta, VaR, concentration risk, etc. |
| `risk_heuristics.txt` | Beta bands, category weights, concentration thresholds, VaR interpretation. |
| `portfolio_rules.txt` | Asset limits (e.g. 5 NSE/MF, 3 COMMODITY), diversification, rebalancing. |
| `visualization_semantics.txt` | Meaning of line/doughnut/bar/scatter charts, time ranges, anomaly cues. |

**Method:** `retrieveContext(String query, String currentPage, String chartType)`:

- Scans the query (and optionally chartType) for keywords (e.g. “risk”, “portfolio”, “chart”).
- Loads corresponding file contents (or in-memory defaults if files are missing) and concatenates them.
- For charts, may add chart-type-specific hints (e.g. line vs doughnut).
- Returns one combined string for the orchestrator to put into the prompt.

RAG is **keyword-based**; it does not perform embedding or vector search.

---

### 3.6 PortfolioAnalysisService

**File:** `src/main/java/com/portfolio/backend/service/analysis/PortfolioAnalysisService.java`

**Role:** Read-only portfolio analytics used to build the “PORTFOLIO DATA” section for the chatbot.

**Input:** `clientId`, `Map<String, BigDecimal> currentPrices` (symbol → price). If a symbol has no current price, buy price is used.

**Method:** `analyzePortfolio(clientId, currentPrices)`:

- Loads holdings via `ClientHoldingRepository.findByClient_ClientId(clientId)`.
- For each holding: invested = quantity × avgBuyPrice; market value = quantity × currentPrice (or avgBuyPrice); P&L and P&L %.
- Aggregates: total value, total invested, unrealized P&L.
- **Portfolio beta:** weighted sum by category (COMMODITY 1.5, NSE 1.0, MF 0.7) / total value.
- **Category exposure:** value and percentage per asset category.
- **Top/underperformers:** by P&L %.
- **Risk warnings:** e.g. beta > 1.2, commodity > 30%, unrealized loss > 10%.
- **Concentration alerts:** e.g. any category > 50%.

**Output:** `PortfolioAnalysisResult` (and optionally `generatePortfolioSummary(analysis)` as human-readable text for the prompt).

---

### 3.7 Visualization Explanation (Strategy Pattern)

**Package:** `com.portfolio.backend.service.visualization`

**Interface:** `VisualizationExplanationStrategy`  
- `explain(VisualizationMetadata metadata, String portfolioContext)` → String  
- `supports(String chartType)` → boolean  

**Implementations:**

- **LineChartStrategy** – performance over time; how to read trends and volatility.
- **DoughnutChartStrategy** – allocation; concentration and diversification.
- **BarChartStrategy** – comparisons across categories.

**VisualizationExplanationService:**

- Holds a list of strategies (injected by Spring).
- `explainVisualization(metadata, portfolioContext)` selects the first strategy where `supports(metadata.getChartType())`, or a default strategy that returns a generic explanation.
- Adding a new chart type = add a new strategy class; no change to orchestrator or UI contract.

---

### 3.8 Configuration

**File:** `src/main/java/com/portfolio/backend/config/GeminiConfig.java`

- `@ConfigurationProperties(prefix = "gemini")`
- Properties: `api-keys` (comma-separated string), `base-url`, `max-retries`, `timeout-seconds`, `min-confidence-threshold`.
- `@PostConstruct parseApiKeys()` splits the string into `parsedApiKeys` (list).  
- `getApiKeys()` returns that list for LLMService.

**File:** `src/main/resources/application.properties`

- Example: `gemini.api-keys=key1,key2`, `gemini.base-url=https://.../models/gemini-2.5-flash:generateContent`, etc.
- Base URL must match a valid Gemini model (e.g. `gemini-2.5-flash` for v1beta).

---

## 4. Frontend Components

### 4.1 Floating Widget (chatbot.js)

**File:** `Frontend/JavaScriptCode/chatbot.js`

**API base:** `http://localhost:8080/api/chatbot` (CHATBOT_API_BASE).

**Class:** `ChatbotWidget`

- **State:** `isOpen`, `isMaximized`, `currentClientId`, `currentPage`, `visualizationContext`, `chatHistory`.
- **init():** Creates DOM (widget container + toggle button), attaches listeners, loads chat history from `localStorage` if desired.
- **createWidget():** Injects the chat UI: header (“Portfolio Assistant”), maximize and close buttons, message list, suggested-questions area, input textarea, Send button.
- **toggle():** Show/hide panel; when closing, clears maximized state.
- **toggleMaximize():** Toggles `.maximized` on the container; CSS uses `position: fixed; inset: 0` so the panel fills the viewport.
- **updateContext():** Reads `globalClientSelect` value → `currentClientId` (integer or null). Reads `.page-view.active` → `currentPage` (e.g. `dashboard`, `holdings`).
- **sendMessage():**  
  - Updates context, then POSTs to `/chat` with body: `{ message, clientId, currentPage, visualizationContext }`.  
  - On response: appends assistant message (with optional confidence/type), renders markdown via `renderMarkdown(...)`, shows suggested questions.  
  - On error: appends an error message.  
- **setVisualizationContext(chartType, chartId, metadata):** Called when the user clicks a chart; stores metadata so the next send includes it in the request.
- **renderMarkdown(text):** Simple client-side markdown: `##`/`###` → headings, `**` → bold, `•`/`-` lines → `<ul>`/`<li>`, newlines → `<br>`.

**Global:** On `DOMContentLoaded`, one `ChatbotWidget` is created and assigned to `window.chatbot` so app.js can call `setVisualizationContext` and optionally open/focus the chat.

---

### 4.2 Chatbot CSS (chatbot.css)

**File:** `Frontend/Css/chatbot.css`

- **Widget:** Fixed bottom-right; toggle button (circle); panel with header, messages area, suggested questions, input area.
- **Messages:** User bubbles (right, primary color); assistant bubbles (left, panel background); confidence badges; basic markdown styling (headings, lists, code, blockquote).
- **Maximized:** `.chatbot-container.maximized` → `position: fixed; inset: 0; width: 100vw; height: 100vh; z-index: 9999` so it overlays the whole page.
- **Typography and colors** use CSS variables (e.g. `--bg-panel`, `--primary`, `--text-main`) to match the rest of the app.

---

### 4.3 Integration in index.html

- In `<head>`: `<link rel="stylesheet" href="../Css/chatbot.css">`.
- Before `</body>`: `<script src="../JavaScriptCode/chatbot.js"></script>` (after app.js).
- No dedicated “chat” div in HTML; the widget is created and appended by `ChatbotWidget.createWidget()`.

---

### 4.4 Chart → Chatbot (app.js)

**File:** `Frontend/JavaScriptCode/app.js`

When building Chart.js instances (e.g. allocation doughnut, main line chart):

- **options.onClick** is set. On click, if `window.chatbot` exists:
  - Calls `window.chatbot.setVisualizationContext(chartType, chartId, metadata)` with:
    - chartType: `'line'` or `'doughnut'`
    - chartId: `'mainChart'` or `'allocationChart'`
    - metadata: e.g. `xAxis`, `yAxis`, `timeRange`, `hoverData`, `calculatedMetrics`
  - Optionally opens the chat and pre-fills the input with “Explain this allocation chart” / “Explain this performance trend” and sends.

So “Explain this graph” is backed by **structured metadata** (chart type, axes, hover data), not by sending an image to the LLM.

---

## 5. End-to-End Request Flow

1. User selects a client in “Active Client Account” (optional but recommended for portfolio answers).
2. User opens the floating chat (and optionally maximizes it).
3. User may click a chart first, then ask “Explain this graph” (or type any question).
4. On Send:
   - Frontend: `updateContext()` → read `globalClientSelect`, active page; then POST `/api/chatbot/chat` with `message`, `clientId`, `currentPage`, `visualizationContext`.
5. Backend:
   - ChatbotController validates and calls ChatbotService.processChat().
   - buildContext() loads portfolio for `clientId` (if any) and formats it.
   - RAGService.retrieveContext() adds knowledge snippets.
   - If visualizationContext is set, VisualizationExplanationService adds an explanation.
   - Full context (RAG + portfolio + viz) is passed to LLMService.generateResponse().
   - Gemini is called (with retries/fallback); response is parsed into ChatResponse.
   - Suggested questions and optional insights are set; ChatResponse is returned.
6. Frontend receives ChatResponse, renders markdown in a new message bubble, and shows suggested question buttons.

---

## 6. Configuration Summary

| What | Where |
|------|--------|
| Gemini API keys | `application.properties`: `gemini.api-keys=key1,key2` |
| Gemini model URL | `gemini.base-url` (e.g. `.../gemini-2.5-flash:generateContent`) |
| Retries / timeout | `gemini.max-retries`, `gemini.timeout-seconds` |
| Knowledge base | `src/main/resources/knowledge/*.txt` |
| Chatbot API base (frontend) | `chatbot.js`: `CHATBOT_API_BASE = "http://localhost:8080/api/chatbot"` |

---

## 7. Extending the Chatbot

- **New chart type:** Implement `VisualizationExplanationStrategy` and add it to the visualization package; Spring will pick it up.
- **Richer RAG:** Add or edit files under `knowledge/` and extend `RAGService.retrieveContext()` (e.g. more keywords or file names).
- **Different LLM:** Implement a new service that takes (prompt, context) and returns something compatible with `ChatResponse`; swap or branch in ChatbotService.
- **Market data:** Implement `ChatbotService.getCurrentPrices(clientId)` (e.g. call a market data API) and pass the map into `analyzePortfolio` so P&L and “current” metrics reflect live prices.

---

## 8. File Reference

| Layer | Path |
|-------|------|
| Controller | `src/main/java/com/portfolio/backend/controller/ChatbotController.java` |
| DTOs | `src/main/java/com/portfolio/backend/dto/chatbot/*.java` |
| Orchestrator | `src/main/java/com/portfolio/backend/service/chatbot/ChatbotService.java` |
| LLM | `src/main/java/com/portfolio/backend/service/llm/LLMService.java` |
| RAG | `src/main/java/com/portfolio/backend/service/rag/RAGService.java` |
| Portfolio analysis | `src/main/java/com/portfolio/backend/service/analysis/PortfolioAnalysisService.java` |
| Visualization | `src/main/java/com/portfolio/backend/service/visualization/*.java` |
| Gemini config | `src/main/java/com/portfolio/backend/config/GeminiConfig.java` |
| Knowledge base | `src/main/resources/knowledge/*.txt` |
| Frontend widget | `Frontend/JavaScriptCode/chatbot.js` |
| Frontend styles | `Frontend/Css/chatbot.css` |
| Chart integration | `Frontend/JavaScriptCode/app.js` (chart options.onClick) |
| Page inclusion | `Frontend/Pages/index.html` (CSS + script tags) |

This document and the code together define the whole chatbot system from UI to database-backed context and LLM call.
