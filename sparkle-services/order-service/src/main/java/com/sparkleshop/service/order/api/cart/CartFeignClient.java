package com.sparkleshop.service.order.api.cart;

import com.sparkleshop.common.core.model.Result;
import com.sparkleshop.service.order.dto.internal.cart.CartClearItemsRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "cart-service")
public interface CartFeignClient {

    @PostMapping("/cart/internal/clear")
    Result clearCheckedItems(@RequestBody CartClearItemsRequest request);
}
