package com.portfolio.backend.dto.chatbot;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatResponse {
    private String response; 
    private ConfidenceLevel confidence; 
    private ResponseType type; 
    private List<String> insights; 
    private String explanation; 
    private List<String> suggestedQuestions; 
    
    public enum ConfidenceLevel {
        LOW, MEDIUM, HIGH
    }
    
    public enum ResponseType {
        INFORMATIONAL,
        ANALYTICAL,
        EXPERIMENTAL
    }
}
