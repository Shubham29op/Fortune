package com.portfolio.backend.exception;

import java.time.LocalDateTime;

public class ApiErrorResponse {

    private int status;
    private String error;
    private LocalDateTime timestamp;

    public ApiErrorResponse(int status, String error) {
        this.status = status;
        this.error = error;
        this.timestamp = LocalDateTime.now();
    }

    public int getStatus() {
        return status;
    }

    public String getError() {
        return error;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }
}
