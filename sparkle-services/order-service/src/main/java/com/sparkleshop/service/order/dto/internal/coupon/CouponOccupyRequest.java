package com.sparkleshop.service.order.dto.internal.coupon;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CouponOccupyRequest {

    @NotNull
    private Long userId;

    @NotNull
    private Long couponId;

    @NotNull
    private Long orderId;
}
