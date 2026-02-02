package com.hsbc.fortune.portfolio.service;

import com.hsbc.fortune.portfolio.domain.Customer;
import com.hsbc.fortune.portfolio.domain.Holding;
import com.hsbc.fortune.portfolio.domain.Portfolio;
import com.hsbc.fortune.portfolio.dto.CreateHoldingRequest;
import com.hsbc.fortune.portfolio.dto.HoldingDto;
import com.hsbc.fortune.portfolio.dto.PortfolioDto;
import com.hsbc.fortune.portfolio.repository.CustomerRepository;
import com.hsbc.fortune.portfolio.repository.HoldingRepository;
import com.hsbc.fortune.portfolio.repository.PortfolioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PortfolioService {
    private final CustomerRepository customerRepository;
    private final PortfolioRepository portfolioRepository;
    private final HoldingRepository holdingRepository;

    @Transactional(readOnly = true)
    public PortfolioDto getPortfolioByCustomerIdReadOnly(Long customerId) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new RuntimeException("Customer not found"));

        Portfolio portfolio = portfolioRepository.findByCustomerId(customerId).orElse(null);
        
        if (portfolio == null) {
            // Return empty portfolio DTO without creating one
            PortfolioDto dto = new PortfolioDto();
            dto.setCustomerId(customer.getId());
            dto.setCustomerName(customer.getName());
            dto.setClientId(customer.getClientId());
            dto.setTotalInvested(BigDecimal.ZERO);
            dto.setTotalCurrentValue(BigDecimal.ZERO);
            dto.setTotalGain(BigDecimal.ZERO);
            dto.setTotalReturns(BigDecimal.ZERO);
            dto.setSharpeRatio(BigDecimal.ZERO);
            dto.setHoldings(new java.util.ArrayList<>());
            return dto;
        }

        return portfolioToDto(customer, portfolio);
    }

    @Transactional
    public PortfolioDto getPortfolioByCustomerId(Long customerId) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new RuntimeException("Customer not found"));

        Portfolio portfolio = portfolioRepository.findByCustomerId(customerId)
                .orElseGet(() -> {
                    Portfolio newPortfolio = new Portfolio();
                    newPortfolio.setCustomer(customer);
                    return portfolioRepository.save(newPortfolio);
                });

        return portfolioToDto(customer, portfolio);
    }

    private PortfolioDto portfolioToDto(Customer customer, Portfolio portfolio) {
        BigDecimal totalInvested = portfolio.getTotalInvested();
        BigDecimal totalCurrentValue = portfolio.getTotalCurrentValue();
        if (totalInvested == null) totalInvested = BigDecimal.ZERO;
        if (totalCurrentValue == null) totalCurrentValue = BigDecimal.ZERO;

        PortfolioDto dto = new PortfolioDto();
        dto.setCustomerId(customer.getId());
        dto.setCustomerName(customer.getName());
        dto.setClientId(customer.getClientId());
        dto.setTotalInvested(totalInvested);
        dto.setTotalCurrentValue(totalCurrentValue);
        dto.setTotalGain(portfolio.getTotalGain());
        dto.setTotalReturns(portfolio.getTotalReturns());
        dto.setSharpeRatio(calculateSharpeRatio(portfolio));
        dto.setHoldings((portfolio.getHoldings() != null ? portfolio.getHoldings() : new java.util.ArrayList<Holding>()).stream()
                .map(this::toHoldingDto)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toList()));

        return dto;
    }

    @Transactional
    public HoldingDto addHolding(Long customerId, CreateHoldingRequest request) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new RuntimeException("Customer not found"));

        Portfolio portfolio = portfolioRepository.findByCustomerId(customerId)
                .orElseGet(() -> {
                    Portfolio newPortfolio = new Portfolio();
                    newPortfolio.setCustomer(customer);
                    return portfolioRepository.save(newPortfolio);
                });

        Holding holding = new Holding();
        holding.setPortfolio(portfolio);
        holding.setAssetName(request.getAssetName());
        holding.setCategory(request.getCategory());
        holding.setQuantity(request.getQuantity());
        holding.setAvgPrice(request.getAvgPrice());
        holding.setCurrentPrice(request.getCurrentPrice());
        holding.calculateValues();

        holding = holdingRepository.save(holding);
        return toHoldingDto(holding);
    }

    @Transactional
    public void removeHolding(Long holdingId) {
        holdingRepository.deleteById(holdingId);
    }

    private HoldingDto toHoldingDto(Holding holding) {
        if (holding == null) return null;
        HoldingDto dto = new HoldingDto();
        dto.setId(holding.getId());
        dto.setAssetName(holding.getAssetName());
        dto.setCategory(holding.getCategory());
        dto.setQuantity(holding.getQuantity());
        dto.setAvgPrice(holding.getAvgPrice());
        dto.setCurrentPrice(holding.getCurrentPrice());
        dto.setInvestedAmount(holding.getInvestedAmount() != null ? holding.getInvestedAmount() : BigDecimal.ZERO);
        dto.setCurrentValue(holding.getCurrentValue() != null ? holding.getCurrentValue() : BigDecimal.ZERO);
        dto.setGain(holding.getGain());
        dto.setReturns(holding.getReturns());
        return dto;
    }

    /**
     * Calculate Sharpe Ratio for a portfolio
     * Sharpe Ratio = (Portfolio Return - Risk-Free Rate) / Portfolio Volatility
     * 
     * Risk-free rate assumed to be 6% annually (typical for India)
     * Portfolio volatility calculated based on asset category weights and their historical volatilities
     */
    private BigDecimal calculateSharpeRatio(Portfolio portfolio) {
        if (portfolio == null || portfolio.getHoldings() == null || portfolio.getHoldings().isEmpty()) {
            return BigDecimal.ZERO;
        }
        BigDecimal invested = portfolio.getTotalInvested();
        if (invested == null || invested.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }

        // Risk-free rate (6% annually)
        BigDecimal riskFreeRate = BigDecimal.valueOf(6.0);
        
        // Portfolio return (annualized, already in percentage)
        BigDecimal portfolioReturn = portfolio.getTotalReturns();
        if (portfolioReturn == null) portfolioReturn = BigDecimal.ZERO;
        
        // Calculate portfolio volatility based on asset category weights
        BigDecimal portfolioVolatility = calculatePortfolioVolatility(portfolio);
        
        // If volatility is zero or very small, return zero
        if (portfolioVolatility == null || portfolioVolatility.compareTo(BigDecimal.valueOf(0.01)) < 0) {
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
        Map<Holding.AssetCategory, BigDecimal> categoryVolatilities = new HashMap<>();
        categoryVolatilities.put(Holding.AssetCategory.EQUITY, BigDecimal.valueOf(18.0));      // 18% volatility
        categoryVolatilities.put(Holding.AssetCategory.MUTUAL_FUND, BigDecimal.valueOf(15.0));   // 15% volatility
        categoryVolatilities.put(Holding.AssetCategory.CRYPTO, BigDecimal.valueOf(60.0));         // 60% volatility
        categoryVolatilities.put(Holding.AssetCategory.REAL_ESTATE, BigDecimal.valueOf(12.0));     // 12% volatility
        categoryVolatilities.put(Holding.AssetCategory.GOLD, BigDecimal.valueOf(10.0));            // 10% volatility
        categoryVolatilities.put(Holding.AssetCategory.DEBT, BigDecimal.valueOf(5.0));             // 5% volatility
        categoryVolatilities.put(Holding.AssetCategory.CASH, BigDecimal.valueOf(1.0));             // 1% volatility

        BigDecimal totalValue = portfolio.getTotalCurrentValue();
        if (totalValue == null || totalValue.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }

        // Calculate weighted average volatility
        BigDecimal weightedVolatility = BigDecimal.ZERO;
        for (Holding holding : portfolio.getHoldings()) {
            BigDecimal currentVal = holding.getCurrentValue();
            if (currentVal == null) continue;
            BigDecimal weight = currentVal.divide(totalValue, 4, RoundingMode.HALF_UP);
            BigDecimal categoryVolatility = categoryVolatilities.getOrDefault(
                    holding.getCategory(), BigDecimal.valueOf(15.0)); // Default 15%
            weightedVolatility = weightedVolatility.add(weight.multiply(categoryVolatility));
        }

        return weightedVolatility;
    }
}
