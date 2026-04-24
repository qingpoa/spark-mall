package com.sparkleshop.service.cart.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CartSelectRequest {

    @NotNull(message = "skuId 不能为空")
    private Long skuId;

    @NotNull(message = "selected 不能为空")
    private Boolean selected;
}
