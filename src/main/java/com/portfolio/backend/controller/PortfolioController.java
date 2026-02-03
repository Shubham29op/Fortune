package com.portfolio.backend.controller;

import com.portfolio.backend.dto.BuyAssetRequest;
import com.portfolio.backend.entity.ClientHolding;
import com.portfolio.backend.repository.ClientHoldingRepository; // Import this
import com.portfolio.backend.service.PortfolioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/portfolio")
@CrossOrigin(origins = "*")
public class PortfolioController {

    @Autowired
    private PortfolioService portfolioService;
    
    @Autowired
    private ClientHoldingRepository holdingRepository; // Direct access for reading

    // --- EXISTING BUY ENDPOINT ---
    @PostMapping("/buy")
    public ResponseEntity<?> buyAsset(@RequestBody BuyAssetRequest request) {
        try {
            ClientHolding holding = portfolioService.buyAsset(request);
            return ResponseEntity.ok(holding);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // --- NEW: GET HOLDINGS FOR A CLIENT ---
    @GetMapping("/{clientId}")
    public ResponseEntity<List<ClientHolding>> getHoldings(@PathVariable Long clientId) {
        return ResponseEntity.ok(holdingRepository.findByClient_ClientId(clientId));
    }

    // --- NEW: SELL ASSET (Basic Implementation) ---
    @DeleteMapping("/{holdingId}")
    public ResponseEntity<?> sellAsset(@PathVariable Long holdingId) {
        holdingRepository.deleteById(holdingId);
        return ResponseEntity.ok("Sold successfully");
    }
}