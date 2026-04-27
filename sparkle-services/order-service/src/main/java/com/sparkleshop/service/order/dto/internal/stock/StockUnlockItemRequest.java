package com.sparkleshop.service.order.dto.internal.stock;

import lombok.Data;

@Data
public class StockUnlockItemRequest {

    private Long skuId;

    private Integer quantity;
}
