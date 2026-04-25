package com.sparkleshop.service.order.api.product;

import com.sparkleshop.common.core.model.Result;
import com.sparkleshop.service.order.dto.internal.product.ProductSkuSnapshotRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "product-service")
public interface ProductFeignClient {

    @PostMapping("/product/internal/sku/snapshot")
    Result getSkuSnapshots(@RequestBody ProductSkuSnapshotRequest request);
}
