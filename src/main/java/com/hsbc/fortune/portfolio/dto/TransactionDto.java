package com.hsbc.fortune.portfolio.dto;

import com.hsbc.fortune.portfolio.domain.Holding;
import com.hsbc.fortune.portfolio.domain.Transaction;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransactionDto {
    private Long id;
    private String transactionId;
    private String customerName;
    private String clientId;
    private Transaction.TransactionType type;
    private String asset;
    private Holding.AssetCategory category;
    private BigDecimal quantity;
    private BigDecimal price;
    private BigDecimal amount;
    private Transaction.TransactionStatus status;
    private LocalDateTime timestamp;
}
