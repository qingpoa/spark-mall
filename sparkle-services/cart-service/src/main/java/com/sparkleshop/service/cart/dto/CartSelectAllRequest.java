package com.sparkleshop.service.cart.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CartSelectAllRequest {

    @NotNull(message = "selected 不能为空")
    private Boolean selected;
}
