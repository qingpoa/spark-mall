package com.sparkleshop.service.order.dto.internal.coupon;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class CouponValidateRespDTO {

    private Long couponId;

    private BigDecimal discountAmount;

    private Boolean available;
}
