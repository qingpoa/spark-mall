package com.sparkleshop.service.order.dto.internal.coupon;

import lombok.Data;

@Data
public class CouponRollbackRequest {

    private Long userId;

    private Long couponId;

    private Long orderId;
}
