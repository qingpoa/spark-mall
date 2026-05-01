package com.sparkleshop.service.coupon.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CouponReceiveRespVO {

    private Long couponId;

    private String couponCode;

    private LocalDateTime expireTime;
}
