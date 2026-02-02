package com.hsbc.fortune.portfolio.repository;

import com.hsbc.fortune.portfolio.domain.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {
    Optional<Customer> findByClientId(String clientId);

    @Query("SELECT c FROM Customer c LEFT JOIN FETCH c.portfolios WHERE c.id = :id")
    Optional<Customer> findByIdWithPortfolios(Long id);

    List<Customer> findByType(Customer.ClientType type);
}
