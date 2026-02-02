package com.hsbc.fortune.portfolio.repository;

import com.hsbc.fortune.portfolio.domain.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    List<Transaction> findByCustomerIdOrderByTimestampDesc(Long customerId);

    @Query("SELECT COUNT(t) FROM Transaction t WHERE t.timestamp >= :start AND t.timestamp < :end")
    Long countTransactionsBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    List<Transaction> findTop10ByOrderByTimestampDesc();

    List<Transaction> findByTimestampBetween(LocalDateTime start, LocalDateTime end);
}
