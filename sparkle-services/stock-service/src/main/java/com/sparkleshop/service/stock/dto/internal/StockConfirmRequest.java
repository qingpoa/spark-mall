package com.sparkleshop.service.stock.dto.internal;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class StockConfirmRequest {

    @NotBlank(message = "orderNo 不能为空")
    private String orderNo;

    @Valid
    @NotEmpty(message = "items 不能为空")
    private List<StockConfirmItemRequest> items;
}
