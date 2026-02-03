package com.portfolio.backend.service.chatbot;

import com.portfolio.backend.dto.chatbot.ChatRequest;
import com.portfolio.backend.dto.chatbot.ChatResponse;

public interface ChatbotServiceInterface {
    ChatResponse processChat(ChatRequest request);
}
