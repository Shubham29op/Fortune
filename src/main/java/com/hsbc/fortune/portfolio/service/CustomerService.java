package com.hsbc.fortune.portfolio.service;

import com.hsbc.fortune.portfolio.domain.Customer;
import com.hsbc.fortune.portfolio.domain.Portfolio;
import com.hsbc.fortune.portfolio.dto.CustomerSummaryDto;
import com.hsbc.fortune.portfolio.repository.CustomerRepository;
import com.hsbc.fortune.portfolio.repository.PortfolioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CustomerService {
    private final CustomerRepository customerRepository;
    private final PortfolioRepository portfolioRepository;

    @Transactional(readOnly = true)
    public List<CustomerSummaryDto> getAllCustomers() {
        return customerRepository.findAll().stream()
                .map(this::toSummaryDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public CustomerSummaryDto getCustomerById(Long id) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Customer not found"));
        return toSummaryDto(customer);
    }

    @Transactional
    public CustomerSummaryDto createCustomer(com.hsbc.fortune.portfolio.dto.CreateCustomerRequest request) {
        // Check if client ID already exists
        if (customerRepository.findByClientId(request.getClientId()).isPresent()) {
            throw new RuntimeException("Client ID already exists: " + request.getClientId());
        }

        Customer customer = new Customer();
        customer.setName(request.getName());
        customer.setClientId(request.getClientId());
        customer.setEmail(request.getEmail());
        customer.setType(request.getType());
        customer.setRiskLevel(request.getRiskLevel());

        customer = customerRepository.save(customer);
        return toSummaryDto(customer);
    }

    @Transactional
    public void deleteCustomer(Long id) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Customer not found"));
        customerRepository.delete(customer);
    }

    private CustomerSummaryDto toSummaryDto(Customer customer) {
        CustomerSummaryDto dto = new CustomerSummaryDto();
        dto.setId(customer.getId());
        dto.setClientId(customer.getClientId());
        dto.setName(customer.getName());
        dto.setType(customer.getType());
        dto.setRiskLevel(customer.getRiskLevel());
        dto.setJoinDate(customer.getJoinDate());

        Portfolio portfolio = portfolioRepository.findByCustomerId(customer.getId()).orElse(null);
        if (portfolio != null) {
            dto.setPortfolioValue(portfolio.getTotalCurrentValue());
            dto.setInvestedAmount(portfolio.getTotalInvested());
            dto.setTotalGain(portfolio.getTotalGain());
            dto.setTotalReturns(portfolio.getTotalReturns());
            dto.setSharpeRatio(calculateSharpeRatio(portfolio));
            dto.setAssetCount(portfolio.getHoldings().size());
        } else {
            dto.setPortfolioValue(BigDecimal.ZERO);
            dto.setInvestedAmount(BigDecimal.ZERO);
            dto.setTotalGain(BigDecimal.ZERO);
            dto.setTotalReturns(BigDecimal.ZERO);
            dto.setSharpeRatio(BigDecimal.ZERO);
            dto.setAssetCount(0);
        }

        return dto;
    }

    /**
     * Calculate Sharpe Ratio for a portfolio
     * Sharpe Ratio = (Portfolio Return - Risk-Free Rate) / Portfolio Volatility
     */
    private BigDecimal calculateSharpeRatio(Portfolio portfolio) {
        if (portfolio.getHoldings().isEmpty() || portfolio.getTotalInvested().compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }

        // Risk-free rate (6% annually)
        BigDecimal riskFreeRate = BigDecimal.valueOf(6.0);
        
        // Portfolio return (annualized, already in percentage)
        BigDecimal portfolioReturn = portfolio.getTotalReturns();
        
        // Calculate portfolio volatility based on asset category weights
        BigDecimal portfolioVolatility = calculatePortfolioVolatility(portfolio);
        
        // If volatility is zero or very small, return zero
        if (portfolioVolatility.compareTo(BigDecimal.valueOf(0.01)) < 0) {
            return BigDecimal.ZERO;
        }
        
        // Sharpe Ratio = (Return - RiskFreeRate) / Volatility
        BigDecimal excessReturn = portfolioReturn.subtract(riskFreeRate);
        return excessReturn.divide(portfolioVolatility, 4, RoundingMode.HALF_UP);
    }

    /**
     * Calculate portfolio volatility based on asset category weights and their historical volatilities
     */
    private BigDecimal calculatePortfolioVolatility(Portfolio portfolio) {
        // Historical annual volatilities by asset category (in percentage)
        Map<com.hsbc.fortune.portfolio.domain.Holding.AssetCategory, BigDecimal> categoryVolatilities = new HashMap<>();
        categoryVolatilities.put(com.hsbc.fortune.portfolio.domain.Holding.AssetCategory.EQUITY, BigDecimal.valueOf(18.0));
        categoryVolatilities.put(com.hsbc.fortune.portfolio.domain.Holding.AssetCategory.MUTUAL_FUND, BigDecimal.valueOf(15.0));
        categoryVolatilities.put(com.hsbc.fortune.portfolio.domain.Holding.AssetCategory.CRYPTO, BigDecimal.valueOf(60.0));
        categoryVolatilities.put(com.hsbc.fortune.portfolio.domain.Holding.AssetCategory.REAL_ESTATE, BigDecimal.valueOf(12.0));
        categoryVolatilities.put(com.hsbc.fortune.portfolio.domain.Holding.AssetCategory.GOLD, BigDecimal.valueOf(10.0));
        categoryVolatilities.put(com.hsbc.fortune.portfolio.domain.Holding.AssetCategory.DEBT, BigDecimal.valueOf(5.0));
        categoryVolatilities.put(com.hsbc.fortune.portfolio.domain.Holding.AssetCategory.CASH, BigDecimal.valueOf(1.0));

        BigDecimal totalValue = portfolio.getTotalCurrentValue();
        if (totalValue.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }

        // Calculate weighted average volatility
        BigDecimal weightedVolatility = BigDecimal.ZERO;
        for (com.hsbc.fortune.portfolio.domain.Holding holding : portfolio.getHoldings()) {
            BigDecimal weight = holding.getCurrentValue()
                    .divide(totalValue, 4, RoundingMode.HALF_UP);
            BigDecimal categoryVolatility = categoryVolatilities.getOrDefault(
                    holding.getCategory(), BigDecimal.valueOf(15.0));
            weightedVolatility = weightedVolatility.add(weight.multiply(categoryVolatility));
        }

        return weightedVolatility;
    }
}
