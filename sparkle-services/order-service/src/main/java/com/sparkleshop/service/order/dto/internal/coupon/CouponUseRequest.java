package com.sparkleshop.service.order.dto.internal.coupon;

import lombok.Data;

@Data
public class CouponUseRequest {

    private Long userId;

    private Long couponId;

    private Long orderId;
}
