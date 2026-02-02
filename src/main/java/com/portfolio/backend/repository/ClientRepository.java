
package com.portfolio.backend.repository;

import com.portfolio.backend.entity.Client;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ClientRepository extends JpaRepository<Client, Long> {
    
    // This allows you to find all clients belonging to a specific manager
    // (Useful for the dropdown menu later)
    List<Client> findByManagerId(Long managerId);
}