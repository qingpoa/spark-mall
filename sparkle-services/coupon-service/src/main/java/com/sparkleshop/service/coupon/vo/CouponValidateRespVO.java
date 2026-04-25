package com.sparkleshop.service.coupon.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class CouponValidateRespVO {

    private Long couponId;

    private BigDecimal discountAmount;

    private Boolean available;
}
