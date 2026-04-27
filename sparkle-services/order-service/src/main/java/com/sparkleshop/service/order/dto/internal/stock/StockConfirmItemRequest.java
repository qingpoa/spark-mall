package com.sparkleshop.service.order.dto.internal.stock;

import lombok.Data;

@Data
public class StockConfirmItemRequest {

    private Long skuId;

    private Integer quantity;
}
