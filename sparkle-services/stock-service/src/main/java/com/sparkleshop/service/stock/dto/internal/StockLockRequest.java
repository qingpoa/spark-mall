package com.sparkleshop.service.stock.dto.internal;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class StockLockRequest {

    @NotBlank(message = "orderNo 不能为空")
    private String orderNo;

    @Valid
    @NotEmpty(message = "items 不能为空")
    private List<StockLockItemRequest> items;
}
