package com.hsbc.fortune.portfolio.config;

import com.hsbc.fortune.portfolio.domain.*;
import com.hsbc.fortune.portfolio.repository.CustomerRepository;
import com.hsbc.fortune.portfolio.repository.HoldingRepository;
import com.hsbc.fortune.portfolio.repository.PortfolioRepository;
import com.hsbc.fortune.portfolio.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Component
@Profile("!test")
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {
    private final CustomerRepository customerRepository;
    private final PortfolioRepository portfolioRepository;
    private final HoldingRepository holdingRepository;
    private final TransactionRepository transactionRepository;

    @Override
    public void run(String... args) {
        if (customerRepository.count() > 0) {
            return;
        }

        Customer customer1 = createCustomer("CL001", "Rajesh Kumar", Customer.ClientType.HNW, Customer.RiskLevel.MODERATE);
        Customer customer2 = createCustomer("CL002", "Priya Sharma", Customer.ClientType.REGULAR, Customer.RiskLevel.LOW);
        Customer customer3 = createCustomer("CL003", "Amit Patel", Customer.ClientType.HNW, Customer.RiskLevel.MODERATE);

        Portfolio portfolio1 = createPortfolio(customer1);
        Portfolio portfolio2 = createPortfolio(customer2);
        Portfolio portfolio3 = createPortfolio(customer3);

        createHolding(portfolio1, "HDFC Bank", Holding.AssetCategory.EQUITY, 8500, 1580, 1666);
        createHolding(portfolio1, "Reliance Industries", Holding.AssetCategory.EQUITY, 5200, 2650, 2769);
        createHolding(portfolio2, "SBI Bluechip Fund", Holding.AssetCategory.MUTUAL_FUND, 85000, 58, 62);
        createHolding(portfolio2, "Gold ETF", Holding.AssetCategory.GOLD, 22000, 60, 64);
        createHolding(portfolio3, "Corporate Bonds", Holding.AssetCategory.DEBT, 25000, 1000, 1015);
        createHolding(portfolio3, "Bitcoin", Holding.AssetCategory.CRYPTO, new BigDecimal("0.28"), new BigDecimal("3200000"), new BigDecimal("3500000"));

        createTransaction(customer1, Transaction.TransactionType.BUY, "HDFC Bank", Holding.AssetCategory.EQUITY, 150, new BigDecimal("1666.67"), new BigDecimal("250000"), Transaction.TransactionStatus.SUCCESS);
        createTransaction(customer2, Transaction.TransactionType.SELL, "Reliance Industries", Holding.AssetCategory.EQUITY, 65, new BigDecimal("2769.23"), new BigDecimal("180000"), Transaction.TransactionStatus.SUCCESS);
        createTransaction(customer3, Transaction.TransactionType.BUY, "Gold ETF", Holding.AssetCategory.GOLD, 5000, new BigDecimal("64"), new BigDecimal("320000"), Transaction.TransactionStatus.PENDING);
    }

    private Customer createCustomer(String clientId, String name, Customer.ClientType type, Customer.RiskLevel riskLevel) {
        Customer customer = new Customer();
        customer.setClientId(clientId);
        customer.setName(name);
        customer.setType(type);
        customer.setRiskLevel(riskLevel);
        customer.setJoinDate(LocalDateTime.now().minusMonths(12));
        return customerRepository.save(customer);
    }

    private Portfolio createPortfolio(Customer customer) {
        Portfolio portfolio = new Portfolio();
        portfolio.setCustomer(customer);
        return portfolioRepository.save(portfolio);
    }

    private void createHolding(Portfolio portfolio, String assetName, Holding.AssetCategory category, Number quantity, Number avgPrice, Number currentPrice) {
        Holding holding = new Holding();
        holding.setPortfolio(portfolio);
        holding.setAssetName(assetName);
        holding.setCategory(category);
        holding.setQuantity(new BigDecimal(quantity.toString()));
        holding.setAvgPrice(new BigDecimal(avgPrice.toString()));
        holding.setCurrentPrice(new BigDecimal(currentPrice.toString()));
        holding.calculateValues();
        holdingRepository.save(holding);
    }

    private void createTransaction(Customer customer, Transaction.TransactionType type, String asset, Holding.AssetCategory category, Number quantity, BigDecimal price, BigDecimal amount, Transaction.TransactionStatus status) {
        Transaction transaction = new Transaction();
        transaction.setCustomer(customer);
        transaction.setType(type);
        transaction.setAsset(asset);
        transaction.setCategory(category);
        transaction.setQuantity(new BigDecimal(quantity.toString()));
        transaction.setPrice(price);
        transaction.setAmount(amount);
        transaction.setStatus(status);
        transaction.setTimestamp(LocalDateTime.now());
        transactionRepository.save(transaction);
    }
}
