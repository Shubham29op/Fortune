package com.portfolio.backend.dto.chatbot;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatRequest {
    private String message;
    private Long clientId; 
    private VisualizationMetadata visualizationContext; 
    private String currentPage; 
}
