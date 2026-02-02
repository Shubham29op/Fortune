package com.hsbc.fortune.portfolio.service;

import com.hsbc.fortune.portfolio.domain.Holding;
import com.hsbc.fortune.portfolio.domain.Transaction;
import com.hsbc.fortune.portfolio.dto.*;
import com.hsbc.fortune.portfolio.repository.PortfolioRepository;
import com.hsbc.fortune.portfolio.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardService {
    private final CustomerService customerService;
    private final PortfolioService portfolioService;
    private final TransactionRepository transactionRepository;
    private final PortfolioRepository portfolioRepository;

    @Transactional(readOnly = true)
    public DashboardSummaryDto getDashboardSummary() {
        List<CustomerSummaryDto> allCustomers = customerService.getAllCustomers();

        BigDecimal totalAUM = allCustomers.stream()
                .map(CustomerSummaryDto::getPortfolioValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        int activeClients = allCustomers.size();

        BigDecimal avgReturns = allCustomers.stream()
                .map(CustomerSummaryDto::getTotalReturns)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(activeClients > 0 ? activeClients : 1), 2, RoundingMode.HALF_UP);

        BigDecimal avgSharpeRatio = allCustomers.stream()
                .map(CustomerSummaryDto::getSharpeRatio)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(activeClients > 0 ? activeClients : 1), 4, RoundingMode.HALF_UP);

        LocalDateTime startOfToday = LocalDate.now().atStartOfDay();
        LocalDateTime startOfTomorrow = LocalDate.now().plusDays(1).atStartOfDay();
        Long transactionsToday = transactionRepository.countTransactionsBetween(startOfToday, startOfTomorrow);

        List<CustomerSummaryDto> topClients = allCustomers.stream()
                .sorted((a, b) -> b.getTotalReturns().compareTo(a.getTotalReturns()))
                .limit(5)
                .collect(Collectors.toList());

        List<TransactionDto> recentTransactions = transactionRepository.findTop10ByOrderByTimestampDesc()
                .stream()
                .map(this::toTransactionDto)
                .collect(Collectors.toList());

        Map<String, BigDecimal> assetAllocation = calculateAssetAllocation(allCustomers);

        DashboardSummaryDto summary = new DashboardSummaryDto();
        summary.setTotalAUM(totalAUM);
        summary.setActiveClients(activeClients);
        summary.setAvgReturns(avgReturns);
        summary.setAvgSharpeRatio(avgSharpeRatio);
        summary.setTransactionsToday(transactionsToday != null ? transactionsToday.intValue() : 0);
        summary.setTopClients(topClients);
        summary.setRecentTransactions(recentTransactions);
        summary.setAssetAllocation(assetAllocation);

        return summary;
    }

    private Map<String, BigDecimal> calculateAssetAllocation(List<CustomerSummaryDto> customers) {
        Map<String, BigDecimal> allocation = new HashMap<>();
        BigDecimal totalValue = BigDecimal.ZERO;

        for (CustomerSummaryDto customer : customers) {
            // Use read-only method to avoid creating portfolios in read-only transaction
            PortfolioDto portfolio = portfolioService.getPortfolioByCustomerIdReadOnly(customer.getId());
            if (portfolio != null && portfolio.getHoldings() != null) {
                for (HoldingDto holding : portfolio.getHoldings()) {
                    String category = holding.getCategory().name();
                    BigDecimal currentValue = holding.getCurrentValue() != null ? holding.getCurrentValue() : BigDecimal.ZERO;
                    allocation.put(category, allocation.getOrDefault(category, BigDecimal.ZERO).add(currentValue));
                    totalValue = totalValue.add(currentValue);
                }
            }
        }

        if (totalValue.compareTo(BigDecimal.ZERO) > 0) {
            Map<String, BigDecimal> percentages = new HashMap<>();
            for (Map.Entry<String, BigDecimal> entry : allocation.entrySet()) {
                BigDecimal percentage = entry.getValue()
                        .divide(totalValue, 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100));
                percentages.put(entry.getKey(), percentage);
            }
            return percentages;
        }

        return allocation;
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
