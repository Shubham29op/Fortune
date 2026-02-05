# Testing Strategy and Unit Test Coverage

This document describes the current testing approach for the Fortune Pro backend and outlines the main unit test scenarios. The goal is to keep the test suite focused, fast, and directly aligned with the core business logic of the system.

## 1. Testing Approach

- Use JUnit 5 and Spring Boot test support.
- Favour service-level unit tests over heavy end‑to‑end tests.
- Keep tests deterministic; avoid real network calls and external systems.
- Treat the database as an implementation detail in unit tests by mocking repositories.

The existing `BackendApplicationTests` class is used only to verify that the Spring context starts successfully. All business logic is covered in dedicated service tests.

## 2. PortfolioService Tests

**Class under test:** `PortfolioService`

**Collaborators:** `ClientRepository`, `AssetRepository`, `ClientHoldingRepository`

**Key scenarios:**

1. **Buy asset for existing client and asset**
   - Given a valid `BuyAssetRequest` with existing client and asset.
   - When `buyAsset` is called.
   - Then a new `ClientHolding` is created with the correct quantity, average buy price, and client/asset references.

2. **Reject buy when client does not exist**
   - Given a `BuyAssetRequest` with a non‑existent client ID.
   - When `buyAsset` is called.
   - Then a `ClientNotFoundException` is thrown.

3. **Reject buy when asset does not exist**
   - Given a `BuyAssetRequest` with a non‑existent asset ID.
   - When `buyAsset` is called.
   - Then an `AssetNotFoundException` is thrown.

4. **Category‑based risk rules**
   - Given a client that already holds the maximum allowed assets in a category.
   - When another asset of the same category is requested.
   - Then a `BusinessRuleViolationException` is thrown with a clear message.

5. **Closing a holding**
   - Given an existing `ClientHolding`.
   - When the holding is deleted via the service.
   - Then the repository is called with the correct ID and no other holdings are affected.

## 3. PortfolioAnalysisService Tests

**Class under test:** `PortfolioAnalysisService`

**Collaborators:** `ClientHoldingRepository`

**Key scenarios:**

1. **Empty portfolio**
   - Given a client with no holdings.
   - When `analyzePortfolio` is called.
   - Then the returned `PortfolioAnalysisResult` has zero totals, beta of 1.0, and empty warning lists.

2. **Single holding with known values**
   - Given one holding with a fixed quantity and buy price.
   - When a current price is supplied via the `currentPrices` map.
   - Then total invested, total value, unrealised P&L, and category allocations are calculated correctly.

3. **Beta calculation by category**
   - Given holdings across NSE, MF, and COMMODITY with different weights.
   - When `analyzePortfolio` is called.
   - Then the calculated `portfolioBeta` respects the configured category weights.

4. **Risk warnings**
   - Given analysis results with beta greater than 1.2.
   - Given commodity allocation above 30%.
   - Given unrealised loss above 10% of invested capital.
   - Then `generateRiskWarnings` returns a list containing all relevant warning strings.

5. **Concentration alerts**
   - Given category percentages where any category exceeds 50%.
   - Then `generateConcentrationAlerts` returns human‑readable alerts for those categories.

6. **Summary generation**
   - Given a populated `PortfolioAnalysisResult`.
   - When `generatePortfolioSummary` is called.
   - Then the markdown summary includes total value, invested amount, unrealised P&L, beta, allocations, and any warnings or alerts.

## 4. ChatbotService and LLMService Tests

**Classes under test:** `ChatbotService`, `LLMService`

**Collaborators:** `PortfolioAnalysisService`, `RAGService`, `VisualizationExplanationService`, `GeminiConfig`, `WebClient`

**ChatbotService scenarios:**

1. **Context construction with client and page**
   - Given a `ChatRequest` with `clientId` and `currentPage`.
   - When `processChat` is called.
   - Then the built context string includes portfolio summary and the current page label.

2. **Visualisation explanation enrichment**
   - Given a `ChatRequest` with a `VisualizationMetadata` payload.
   - When `processChat` is called.
   - Then the context passed to the LLM is prefixed with the visualisation explanation returned by `VisualizationExplanationService`.

3. **Portfolio insights enrichment**
   - Given a `ChatResponse` without insights and a valid `clientId`.
   - When `processChat` completes.
   - Then any risk warnings from `PortfolioAnalysisService` are added to the response insights.

4. **Error handling**
   - Given an exception thrown by a dependency.
   - When `processChat` is called.
   - Then an error response is returned with low confidence and informational type.

**LLMService scenarios (with WebClient mocked):**

1. **Successful Gemini call**
   - Given a valid prompt and a configured API key.
   - When `generateResponse` is called.
   - Then the JSON response is parsed into a `ChatResponse` with formatted markdown and a non‑null confidence value.

2. **Retry on transient errors**
   - Given a 429 or 5xx response from Gemini.
   - When `generateResponse` is called.
   - Then the service retries up to `maxRetries` before falling back.

3. **Fallback response when API is unavailable**
   - Given repeated failures from Gemini.
   - When `generateResponse` is called.
   - Then a clear fallback message is returned, marked as informational with low confidence.

## 5. RAGService Tests

**Class under test:** `RAGService`

**Key scenarios:**

1. **Keyword‑based retrieval**
   - Given a query containing risk‑related terms.
   - When `retrieveContext` is called.
   - Then the returned context contains content from the risk heuristics knowledge file.

2. **Chart‑type specific context**
   - Given a query for a visualisation and a specific `chartType`.
   - When `retrieveContext` is called.
   - Then the response includes generic visualisation semantics and the chart‑type specific explanation.

3. **Graceful fallback when files are missing**
   - Given no physical knowledge files on the classpath.
   - When the service is initialised.
   - Then the default in‑code knowledge strings are loaded and used in subsequent calls.

## 6. Frontend Behaviour Checks (High Level)

While the primary test focus is on backend services, the following frontend behaviours are verified manually or via lightweight UI checks:

- CSV export of active holdings and realised trade history.
- Chatbot widget opening/closing, sending messages, and rendering responses with the AI disclaimer.
- Chart interactions where clicking on allocation, performance trend, monthly P&L, or top holdings triggers a contextual chatbot explanation.
- Ticker visibility and colour contrast on the dashboard header.

This document should be updated whenever new service classes are introduced or when existing services gain non‑trivial behaviour that merits explicit unit tests.

