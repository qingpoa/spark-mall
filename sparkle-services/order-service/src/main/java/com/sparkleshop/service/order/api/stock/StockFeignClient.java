package com.sparkleshop.service.order.api.stock;

import com.sparkleshop.common.core.model.Result;
import com.sparkleshop.service.order.dto.internal.stock.StockLockRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "stock-service")
public interface StockFeignClient {

    @PostMapping("/stock/internal/lock")
    Result lockStock(@RequestBody StockLockRequest request);
}
