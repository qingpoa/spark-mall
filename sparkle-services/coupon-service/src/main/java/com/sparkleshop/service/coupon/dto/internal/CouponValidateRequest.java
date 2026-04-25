package com.sparkleshop.service.coupon.dto.internal;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CouponValidateRequest {

    @NotNull(message = "userId 不能为空")
    private Long userId;

    @NotNull(message = "couponId 不能为空")
    private Long couponId;

    @NotNull(message = "orderAmount 不能为空")
    private BigDecimal orderAmount;
}
