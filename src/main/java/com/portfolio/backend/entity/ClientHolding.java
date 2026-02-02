package com.portfolio.backend.entity;

import jakarta.persistence.*; // Imports @Entity, @Id, @ManyToOne, etc.
import lombok.Data;           // Imports @Data for Getters/Setters
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "client_holdings")
@Data // Generates Getters and Setters automatically
public class ClientHolding {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long holdingId;

    @ManyToOne
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    @ManyToOne
    @JoinColumn(name = "asset_id", nullable = false)
    private Asset asset;

    private BigDecimal quantity;
    
    private BigDecimal avgBuyPrice;
    
    private LocalDate buyDate;
}