package com.sparkleshop.service.product.dto.admin;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AdminSpuStatusUpdateRequest {

    @NotNull
    @Min(0)
    @Max(1)
    private Integer status;
}
