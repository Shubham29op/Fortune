package com.portfolio.backend.controller;

import com.portfolio.backend.dto.BuyAssetRequest;
import com.portfolio.backend.entity.ClientHolding;
import com.portfolio.backend.service.PortfolioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/portfolio")
@CrossOrigin(origins = "*") // Allows Frontend to access this
public class PortfolioController {

    @Autowired
    private PortfolioService portfolioService;

    @PostMapping("/buy")
    public ResponseEntity<?> buyAsset(@RequestBody BuyAssetRequest request) {
        try {
            ClientHolding holding = portfolioService.buyAsset(request);
            return ResponseEntity.ok(holding);
        } catch (RuntimeException e) {
            // Returns a 400 Bad Request with the specific error message (e.g., "Limit Reached")
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}