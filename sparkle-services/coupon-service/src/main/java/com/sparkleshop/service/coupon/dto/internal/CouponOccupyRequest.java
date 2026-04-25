package com.sparkleshop.service.coupon.dto.internal;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CouponOccupyRequest {

    @NotNull(message = "userId 不能为空")
    private Long userId;

    @NotNull(message = "couponId 不能为空")
    private Long couponId;

    @NotNull(message = "orderId 不能为空")
    private Long orderId;
}
