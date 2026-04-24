package com.sparkleshop.service.cart.dto.internal;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class ProductSkuSnapshotRequest {

    @NotEmpty(message = "skuIds 不能为空")
    private List<Long> skuIds;
}
