package com.sparkleshop.service.order.api.user;

import com.sparkleshop.common.core.model.Result;
import com.sparkleshop.service.order.dto.internal.user.AddressDetailRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "user-service")
public interface UserFeignClient {

    @PostMapping("/user/internal/address/detail")
    Result getAddressDetail(@RequestBody AddressDetailRequest request);
}
