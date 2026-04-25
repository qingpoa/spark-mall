package com.sparkleshop.service.order.dto.internal.product;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class ProductSkuSnapshotRequest {

    @NotEmpty
    private List<@NotNull Long> skuIds;
}
