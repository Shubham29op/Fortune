package com.hsbc.fortune.portfolio.repository;

import com.hsbc.fortune.portfolio.domain.Portfolio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PortfolioRepository extends JpaRepository<Portfolio, Long> {
    @Query("SELECT p FROM Portfolio p LEFT JOIN FETCH p.holdings WHERE p.customer.id = :customerId")
    Optional<Portfolio> findByCustomerId(Long customerId);
}
