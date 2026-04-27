package com.sparkleshop.service.order.api.stock;

import com.sparkleshop.common.core.model.Result;
import com.sparkleshop.service.order.dto.internal.stock.StockConfirmRequest;
import com.sparkleshop.service.order.dto.internal.stock.StockLockRequest;
import com.sparkleshop.service.order.dto.internal.stock.StockUnlockRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "stock-service")
public interface StockFeignClient {

    @PostMapping("/stock/internal/lock")
    Result lockStock(@RequestBody StockLockRequest request);

    @PostMapping("/stock/internal/confirm")
    Result confirmStock(@RequestBody StockConfirmRequest request);

    @PostMapping("/stock/internal/unlock")
    Result unlockStock(@RequestBody StockUnlockRequest request);
}
