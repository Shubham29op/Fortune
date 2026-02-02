package com.hsbc.fortune.portfolio.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "portfolios")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Portfolio {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "portfolio", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Holding> holdings = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public BigDecimal getTotalInvested() {
        if (holdings == null) return BigDecimal.ZERO;
        return holdings.stream()
                .map(Holding::getInvestedAmount)
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal getTotalCurrentValue() {
        if (holdings == null) return BigDecimal.ZERO;
        return holdings.stream()
                .map(Holding::getCurrentValue)
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal getTotalGain() {
        BigDecimal current = getTotalCurrentValue();
        BigDecimal invested = getTotalInvested();
        if (current == null) current = BigDecimal.ZERO;
        if (invested == null) invested = BigDecimal.ZERO;
        return current.subtract(invested);
    }

    public BigDecimal getTotalReturns() {
        BigDecimal invested = getTotalInvested();
        if (invested == null || invested.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal gain = getTotalGain();
        if (gain == null) gain = BigDecimal.ZERO;
        return gain.divide(invested, 4, java.math.RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
    }
}
