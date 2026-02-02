package com.hsbc.fortune.portfolio.dto;

import com.hsbc.fortune.portfolio.domain.Customer;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateCustomerRequest {
    @NotBlank(message = "Name is required")
    private String name;

    @NotBlank(message = "Client ID is required")
    private String clientId;

    private String email;

    @NotNull(message = "Client type is required")
    private Customer.ClientType type;

    @NotNull(message = "Risk level is required")
    private Customer.RiskLevel riskLevel;
}
