package com.sparkleshop.service.product.dto.internal;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class ProductSkuSnapshotQueryRequest {

    @NotEmpty(message = "skuIds 不能为空")
    private List<@NotNull(message = "skuId 不能为空") Long> skuIds;
}
