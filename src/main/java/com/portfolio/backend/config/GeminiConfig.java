package com.portfolio.backend.config;

import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

/**
 * Configuration for Gemini API endpoints
 * Supports multiple API keys for fallback
 */
@Configuration
@ConfigurationProperties(prefix = "gemini")
@Data
@Slf4j
public class GeminiConfig {
    private String apiKeys; 
    private String baseUrl = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent";
    private int maxRetries = 3;
    private int timeoutSeconds = 30;
    private double minConfidenceThreshold = 0.6; 
    
    private List<String> parsedApiKeys = new ArrayList<>(); // Parsed API keys
    

    @PostConstruct
    public void parseApiKeys() {
        if (apiKeys != null && !apiKeys.trim().isEmpty() && !apiKeys.startsWith("YOUR_API_KEY")) {
            
            String[] keyArray = apiKeys.split(",");
            parsedApiKeys = new ArrayList<>();
            for (String key : keyArray) {
                String trimmedKey = key.trim();
                if (!trimmedKey.isEmpty()) {
                    parsedApiKeys.add(trimmedKey);
                }
            }
            log.info("Loaded {} Gemini API key(s) for fallback", parsedApiKeys.size());
        } else {
            log.warn("No valid Gemini API keys found in configuration. Chatbot will use fallback responses.");
            parsedApiKeys = new ArrayList<>();
        }
    }
    
   
    public List<String> getApiKeys() {
        return parsedApiKeys;
    }
}
