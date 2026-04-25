package com.sparkleshop.service.order.dto.internal.stock;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class StockLockRequest {

    @NotBlank
    private String orderNo;

    @Valid
    @NotEmpty
    private List<StockLockItemRequest> items;
}
