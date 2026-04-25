package com.sparkleshop.service.order.dto.internal.cart;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class CartClearItemsRequest {

    @NotNull
    private Long userId;

    @NotEmpty
    private List<@NotNull Long> skuIds;
}
