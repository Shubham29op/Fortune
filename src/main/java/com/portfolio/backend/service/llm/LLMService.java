package com.portfolio.backend.service.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.portfolio.backend.config.GeminiConfig;
import com.portfolio.backend.dto.chatbot.ChatResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * LLM Service with Gemini API integration and fallback strategy
 * Handles retries, rate limits, and multiple API keys
 */
@Service
@Slf4j
public class LLMService implements LLMServiceInterface {
    
    private final GeminiConfig config;
    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final AtomicInteger currentApiKeyIndex = new AtomicInteger(0);
    
    public LLMService(GeminiConfig config) {
        this.config = config;
        this.objectMapper = new ObjectMapper();
        this.webClient = WebClient.builder()
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
        
        // Initialize API keys if not configured
        if (config.getApiKeys() == null || config.getApiKeys().isEmpty()) {
            log.warn("No Gemini API keys configured. Chatbot will use fallback responses.");
        }
    }
    
    /**
     * Generate response using Gemini API with fallback strategy
     */
    public ChatResponse generateResponse(String prompt, String context) {
        String fullPrompt = buildPrompt(prompt, context);
        
        // Try each API key with retries
        for (int attempt = 0; attempt < config.getMaxRetries(); attempt++) {
            String apiKey = getNextApiKey();
            
            try {
                return callGeminiAPI(fullPrompt, apiKey);
            } catch (Exception e) {
                log.warn("Gemini API call failed (attempt {}): {}", attempt + 1, e.getMessage());
                
                if (isRetryableError(e) && attempt < config.getMaxRetries() - 1) {
                    try {
                        Thread.sleep(1000 * (attempt + 1)); // Exponential backoff
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    }
                    continue;
                }
                
                // If all retries exhausted, try next API key or return fallback
                if (attempt == config.getMaxRetries() - 1) {
                    return createFallbackResponse(prompt);
                }
            }
        }
        
        return createFallbackResponse(prompt);
    }
    
    private ChatResponse callGeminiAPI(String prompt, String apiKey) {
        
        Map<String, Object> requestBody = new HashMap<>();
        
       
        Map<String, Object> part = new HashMap<>();
        part.put("text", prompt);
        
        Map<String, Object> content = new HashMap<>();
        content.put("parts", List.of(part));
        
        requestBody.put("contents", List.of(content));
        
       
        List<Map<String, String>> safetySettings = List.of(
            Map.of("category", "HARM_CATEGORY_HARASSMENT", "threshold", "BLOCK_MEDIUM_AND_ABOVE"),
            Map.of("category", "HARM_CATEGORY_HATE_SPEECH", "threshold", "BLOCK_MEDIUM_AND_ABOVE"),
            Map.of("category", "HARM_CATEGORY_SEXUALLY_EXPLICIT", "threshold", "BLOCK_MEDIUM_AND_ABOVE"),
            Map.of("category", "HARM_CATEGORY_DANGEROUS_CONTENT", "threshold", "BLOCK_MEDIUM_AND_ABOVE")
        );
        requestBody.put("safetySettings", safetySettings);
        
        String url = config.getBaseUrl() + "?key=" + apiKey;
        
        log.debug("Calling Gemini API: {}", url);
        log.debug("Request body: {}", requestBody);
        
        try {
            String response = webClient.post()
                    .uri(url)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(config.getTimeoutSeconds()))
                    .retryWhen(Retry.backoff(2, Duration.ofSeconds(1))
                            .filter(this::isRetryableError)
                            .doBeforeRetry(retrySignal -> 
                                log.info("Retrying Gemini API call..."))
                    )
                    .block();
            
            log.debug("Gemini API response: {}", response);
            return parseGeminiResponse(response);
            
        } catch (WebClientResponseException.TooManyRequests e) {
            log.warn("Rate limit hit (429), will retry with next API key. Response: {}", e.getResponseBodyAsString());
            throw new RuntimeException("Rate limit exceeded", e);
            
        } catch (WebClientResponseException e) {
            String errorBody = e.getResponseBodyAsString();
            int statusCode = e.getStatusCode().value();
            log.error("Gemini API error ({}): {}", statusCode, errorBody);
            log.error("Request URL: {}", url);
            log.error("Request body: {}", requestBody);
            
            // Check for specific error types
            if (statusCode == 400) {
                log.error("Bad Request - Check API key format and request structure");
            } else if (statusCode == 401) {
                log.error("Unauthorized - Invalid API key");
            } else if (statusCode == 403) {
                log.error("Forbidden - API key may not have required permissions");
            }
            
            throw new RuntimeException("API call failed: " + statusCode + " - " + errorBody, e);
            
        } catch (Exception e) {
            log.error("Unexpected error calling Gemini API", e);
            throw new RuntimeException("Unexpected API error", e);
        }
    }
    
    private ChatResponse parseGeminiResponse(String responseJson) {
        try {
            JsonNode root = objectMapper.readTree(responseJson);
            JsonNode candidates = root.path("candidates");
            
            if (candidates.isEmpty() || !candidates.has(0)) {
                return createFallbackResponse("Unable to parse response");
            }
            
            JsonNode content = candidates.get(0).path("content");
            JsonNode parts = content.path("parts");
            
            if (parts.isEmpty() || !parts.has(0)) {
                return createFallbackResponse("No content in response");
            }
            
            String text = parts.get(0).path("text").asText("");
            
            // Check confidence (simplified - in production, use actual confidence scores)
            ChatResponse.ConfidenceLevel confidence = estimateConfidence(text);
            
            ChatResponse response = new ChatResponse();
            response.setResponse(formatResponse(text));
            response.setConfidence(confidence);
            response.setType(ChatResponse.ResponseType.ANALYTICAL);
            response.setExplanation("Generated by Gemini AI");
            return response;
                    
        } catch (Exception e) {
            log.error("Error parsing Gemini response", e);
            return createFallbackResponse("Response parsing failed");
        }
    }
    
    private ChatResponse.ConfidenceLevel estimateConfidence(String text) {
        
        if (text.length() > 200 && text.contains("•") || text.contains("-")) {
            return ChatResponse.ConfidenceLevel.HIGH;
        } else if (text.length() > 100) {
            return ChatResponse.ConfidenceLevel.MEDIUM;
        }
        return ChatResponse.ConfidenceLevel.LOW;
    }
    
    private String formatResponse(String text) {
  
        if (!text.contains("##") && !text.contains("###")) {
           
            return "## Analysis\n\n" + text;
        }
        return text;
    }
    
    private String buildPrompt(String userMessage, String context) {
        StringBuilder prompt = new StringBuilder();
        
        prompt.append("You are a portfolio analyst assistant. Give CRISP, SHORT answers. ");
        prompt.append("Use bullet points; avoid long paragraphs. Never give financial advice.\n\n");
        prompt.append("If PORTFOLIO DATA is provided below, you MUST use it to give specific risks, numbers, and insights. ");
        prompt.append("Do NOT say 'without portfolio data' or 'I cannot identify' when data is provided. ");
        prompt.append("Reference actual figures (e.g. allocation %, beta, P&L) from the data.\n\n");
        prompt.append("STRICT: Under 150 words, 3-5 bullets, one ## heading. Be direct.\n\n");
        
        if (context != null && !context.isEmpty()) {
            prompt.append("CONTEXT / PORTFOLIO DATA:\n").append(context).append("\n\n");
        }
        
        prompt.append("USER QUESTION: ").append(userMessage).append("\n\n");
        prompt.append("Reply concisely using the data above when provided. Main point first, then bullet insights.");
        
        return prompt.toString();
    }
    
    private boolean isRetryableError(Throwable error) {
        if (error instanceof WebClientResponseException) {
            WebClientResponseException e = (WebClientResponseException) error;
            int status = e.getStatusCode().value();
            
            return status == 429 || status == 408 || status >= 500;
        }
        return error instanceof java.util.concurrent.TimeoutException;
    }
    
    private String getNextApiKey() {
        List<String> keys = config.getApiKeys();
        if (keys == null || keys.isEmpty()) {
            log.error("No Gemini API keys available. Check application.properties configuration.");
            throw new IllegalStateException("No Gemini API keys configured");
        }
        
        int index = currentApiKeyIndex.getAndIncrement() % keys.size();
        String selectedKey = keys.get(index);
        log.debug("Using API key {} of {}", index + 1, keys.size());
        return selectedKey;
    }
    
    private ChatResponse createFallbackResponse(String originalPrompt) {
        log.warn("Using fallback response for: {}", originalPrompt);
        ChatResponse response = new ChatResponse();
        response.setResponse("## Service Temporarily Unavailable\n\n" +
                         "I'm currently unable to process your request. " +
                         "Please try again in a moment, or rephrase your question.\n\n" +
                         "**Note:** This is a fallback response. The AI service may be experiencing high load.");
        response.setConfidence(ChatResponse.ConfidenceLevel.LOW);
        response.setType(ChatResponse.ResponseType.INFORMATIONAL);
        response.setExplanation("Fallback response due to API unavailability");
        return response;
    }
}
