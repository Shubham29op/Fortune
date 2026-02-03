package com.portfolio.backend.repository;

import com.portfolio.backend.entity.Manager;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ManagerRepository extends JpaRepository<Manager, Long> {
    Manager findByEmail(String email);
}
