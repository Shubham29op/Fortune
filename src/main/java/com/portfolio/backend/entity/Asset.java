package com.portfolio.backend.entity;

import com.portfolio.backend.entity.enums.AssetCategory;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity
@Table(name = "assets")
@Data // Lombok: Generates Getters, Setters, toString, etc.
@NoArgsConstructor
@AllArgsConstructor
public class Asset {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long assetId;

    @Column(unique = true, nullable = false)
    private String symbol; // e.g., 'RELIANCE', 'GOLD'

    @Column(nullable = false)
    private String assetName; // e.g., 'Reliance Industries', 'Gold 24k'

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AssetCategory category; // NSE, MF, or COMMODITY

    // Optional: Useful for searching later
    private String description;
}