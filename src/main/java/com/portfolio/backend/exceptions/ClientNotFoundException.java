package com.portfolio.backend.exceptions;

public class ClientNotFoundException extends RuntimeException {
    public ClientNotFoundException(Long clientId) {
        super("Client not found with ID: " + clientId);
    }
}
