package com.portfolio.backend.exceptions;

import java.math.BigDecimal;

public class InvalidTransactionException extends RuntimeException {

    public InvalidTransactionException(String message) {
        super(message);
    }

    public static InvalidTransactionException invalidQuantity(BigDecimal qty) {
        return new InvalidTransactionException("Invalid quantity: " + qty);
    }

    public static InvalidTransactionException invalidPrice(BigDecimal price) {
        return new InvalidTransactionException("Invalid price: " + price);
    }
}
