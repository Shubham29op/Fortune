package com.hsbc.fortune.portfolio.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Entity
@Table(name = "holdings")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Holding {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "portfolio_id", nullable = false)
    private Portfolio portfolio;

    @Column(nullable = false, length = 200)
    private String assetName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private AssetCategory category;

    @Column(nullable = false, precision = 18, scale = 4)
    private BigDecimal quantity;

    @Column(name = "avg_price", nullable = false, precision = 18, scale = 2)
    private BigDecimal avgPrice;

    @Column(name = "current_price", nullable = false, precision = 18, scale = 2)
    private BigDecimal currentPrice;

    @Column(name = "invested_amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal investedAmount;

    @Column(name = "current_value", nullable = false, precision = 18, scale = 2)
    private BigDecimal currentValue;

    @PrePersist
    @PreUpdate
    public void calculateValues() {
        if (quantity != null && avgPrice != null) {
            investedAmount = quantity.multiply(avgPrice).setScale(2, RoundingMode.HALF_UP);
        }
        if (quantity != null && currentPrice != null) {
            currentValue = quantity.multiply(currentPrice).setScale(2, RoundingMode.HALF_UP);
        }
    }

    public BigDecimal getGain() {
        if (currentValue != null && investedAmount != null) {
            return currentValue.subtract(investedAmount);
        }
        return BigDecimal.ZERO;
    }

    public BigDecimal getReturns() {
        if (investedAmount == null || investedAmount.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal gain = getGain();
        return gain.divide(investedAmount, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
    }

    public enum AssetCategory {
        EQUITY, DEBT, GOLD, REAL_ESTATE, CRYPTO, CASH, MUTUAL_FUND
    }
}
