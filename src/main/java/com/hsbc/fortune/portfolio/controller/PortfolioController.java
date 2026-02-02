package com.hsbc.fortune.portfolio.controller;

import com.hsbc.fortune.portfolio.dto.CreateHoldingRequest;
import com.hsbc.fortune.portfolio.dto.HoldingDto;
import com.hsbc.fortune.portfolio.dto.PortfolioDto;
import com.hsbc.fortune.portfolio.service.PortfolioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/portfolios")
@Tag(name = "Portfolio Management", description = "Portfolio and holdings management APIs")
@RequiredArgsConstructor
public class PortfolioController {
    private final PortfolioService portfolioService;

    @GetMapping("/customer/{customerId}")
    @Operation(summary = "Get portfolio by customer ID", description = "Retrieve complete portfolio for a customer")
    public ResponseEntity<PortfolioDto> getPortfolioByCustomerId(@PathVariable Long customerId) {
        return ResponseEntity.ok(portfolioService.getPortfolioByCustomerId(customerId));
    }

    @PostMapping("/customer/{customerId}/holdings")
    @Operation(summary = "Add holding to portfolio", description = "Add a new asset holding to customer portfolio")
    public ResponseEntity<HoldingDto> addHolding(
            @PathVariable Long customerId,
            @Valid @RequestBody CreateHoldingRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(portfolioService.addHolding(customerId, request));
    }

    @DeleteMapping("/holdings/{holdingId}")
    @Operation(summary = "Remove holding from portfolio", description = "Delete a holding from portfolio")
    public ResponseEntity<Void> removeHolding(@PathVariable Long holdingId) {
        portfolioService.removeHolding(holdingId);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
