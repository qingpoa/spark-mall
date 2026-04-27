package com.sparkleshop.service.order.dto.internal.stock;

import lombok.Data;

import java.util.List;

@Data
public class StockConfirmRequest {

    private String orderNo;

    private List<StockConfirmItemRequest> items;
}
