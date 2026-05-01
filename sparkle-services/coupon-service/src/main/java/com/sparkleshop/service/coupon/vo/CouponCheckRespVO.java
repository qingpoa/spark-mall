package com.sparkleshop.service.coupon.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class CouponCheckRespVO {

    private Boolean valid;

    private String reason;

    private BigDecimal discountAmount;
}
