package com.portfolio.backend.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "managers")
@Data
public class Manager {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long managerId;

    @Column(nullable = false)
    private String fullName;

    @Column(unique = true, nullable = false)
    private String email;

    private String password; 
}