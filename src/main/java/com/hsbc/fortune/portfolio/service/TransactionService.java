package com.hsbc.fortune.portfolio.service;

import com.hsbc.fortune.portfolio.domain.Customer;
import com.hsbc.fortune.portfolio.domain.Transaction;
import com.hsbc.fortune.portfolio.dto.TransactionDto;
import com.hsbc.fortune.portfolio.repository.CustomerRepository;
import com.hsbc.fortune.portfolio.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TransactionService {
    private final TransactionRepository transactionRepository;
    private final CustomerRepository customerRepository;

    @Transactional(readOnly = true)
    public List<TransactionDto> getAllTransactions() {
        return transactionRepository.findAll().stream()
                .map(this::toTransactionDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<TransactionDto> getTransactionsByCustomerId(Long customerId) {
        return transactionRepository.findByCustomerIdOrderByTimestampDesc(customerId).stream()
                .map(this::toTransactionDto)
                .collect(Collectors.toList());
    }

    private TransactionDto toTransactionDto(Transaction transaction) {
        TransactionDto dto = new TransactionDto();
        dto.setId(transaction.getId());
        dto.setTransactionId(transaction.getTransactionId());
        dto.setCustomerName(transaction.getCustomer().getName());
        dto.setClientId(transaction.getCustomer().getClientId());
        dto.setType(transaction.getType());
        dto.setAsset(transaction.getAsset());
        dto.setCategory(transaction.getCategory());
        dto.setQuantity(transaction.getQuantity());
        dto.setPrice(transaction.getPrice());
        dto.setAmount(transaction.getAmount());
        dto.setStatus(transaction.getStatus());
        dto.setTimestamp(transaction.getTimestamp());
        return dto;
    }
}
