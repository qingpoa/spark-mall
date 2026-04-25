package com.sparkleshop.service.cart.dto.internal;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class CartClearItemsRequest {

    @NotNull(message = "userId 不能为空")
    private Long userId;

    @NotEmpty(message = "skuIds 不能为空")
    private List<@NotNull(message = "skuId 不能为空") Long> skuIds;
}
