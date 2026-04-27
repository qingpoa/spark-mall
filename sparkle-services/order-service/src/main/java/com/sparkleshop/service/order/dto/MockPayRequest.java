package com.sparkleshop.service.order.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class MockPayRequest {

    @NotBlank
    private String payToken;
}
