package com.portfolio.backend.service.rag;

public interface RAGServiceInterface {
    String retrieveContext(String query, String currentPage, String chartType);
}
