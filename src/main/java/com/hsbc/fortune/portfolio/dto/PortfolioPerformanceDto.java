package com.hsbc.fortune.portfolio.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PortfolioPerformanceDto {
    private LocalDate date;
    private BigDecimal portfolioValue;
    private BigDecimal investedAmount;
}
