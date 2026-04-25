package com.sparkleshop.service.order.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SubmitOrderItemRequest {

    @NotNull
    private Long skuId;

    @NotNull
    @Min(1)
    private Integer quantity;
}
