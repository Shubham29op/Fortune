package com.portfolio.backend.controller;

import com.portfolio.backend.dto.chatbot.ChatRequest;
import com.portfolio.backend.dto.chatbot.ChatResponse;
import com.portfolio.backend.service.chatbot.ChatbotServiceInterface;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/chatbot")
@CrossOrigin(origins = "*")
public class ChatbotController {
    
    @Autowired
    private ChatbotServiceInterface chatbotService;
    
    @PostMapping("/chat")
    public ResponseEntity<ChatResponse> chat(@RequestBody ChatRequest request) {
        try {
            if (request.getMessage() == null || request.getMessage().trim().isEmpty()) {
                ChatResponse errorResponse = new ChatResponse();
                errorResponse.setResponse("## Error\n\nPlease provide a message.");
                errorResponse.setConfidence(ChatResponse.ConfidenceLevel.LOW);
                errorResponse.setType(ChatResponse.ResponseType.INFORMATIONAL);
                return ResponseEntity.badRequest().body(errorResponse);
            }
            
            ChatResponse response = chatbotService.processChat(request);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            ChatResponse errorResponse = new ChatResponse();
            errorResponse.setResponse("## Error\n\nAn unexpected error occurred. Please try again.");
            errorResponse.setConfidence(ChatResponse.ConfidenceLevel.LOW);
            errorResponse.setType(ChatResponse.ResponseType.INFORMATIONAL);
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
    
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "healthy", "service", "chatbot"));
    }
}
