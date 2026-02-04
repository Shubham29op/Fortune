package com.portfolio.backend.service.llm;

import com.portfolio.backend.dto.chatbot.ChatResponse;

public interface LLMServiceInterface {
    ChatResponse generateResponse(String prompt, String context);
}
