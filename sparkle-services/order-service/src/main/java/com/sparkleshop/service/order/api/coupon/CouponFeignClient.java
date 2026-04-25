package com.sparkleshop.service.order.api.coupon;

import com.sparkleshop.common.core.model.Result;
import com.sparkleshop.service.order.dto.internal.coupon.CouponOccupyRequest;
import com.sparkleshop.service.order.dto.internal.coupon.CouponValidateRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "coupon-service")
public interface CouponFeignClient {

    @PostMapping("/coupon/internal/validate")
    Result validateCoupon(@RequestBody CouponValidateRequest request);

    @PostMapping("/coupon/internal/occupy")
    Result occupyCoupon(@RequestBody CouponOccupyRequest request);
}
