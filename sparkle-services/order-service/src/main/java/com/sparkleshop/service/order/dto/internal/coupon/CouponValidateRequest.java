package com.sparkleshop.service.order.dto.internal.coupon;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CouponValidateRequest {

    @NotNull
    private Long userId;

    @NotNull
    private Long couponId;

    @NotNull
    private BigDecimal orderAmount;
}
