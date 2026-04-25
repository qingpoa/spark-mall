package com.sparkleshop.service.stock.dto.internal;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class StockLockItemRequest {

    @NotNull(message = "skuId 不能为空")
    private Long skuId;

    @NotNull(message = "quantity 不能为空")
    @Min(value = 1, message = "quantity 必须大于 0")
    private Integer quantity;
}
