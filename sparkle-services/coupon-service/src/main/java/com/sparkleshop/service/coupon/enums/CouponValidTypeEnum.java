package com.sparkleshop.service.coupon.enums;

import lombok.Getter;

@Getter
public enum CouponValidTypeEnum {

    FIXED_DAYS(1, "固定天数"),
    FIXED_RANGE(2, "固定时间范围");

    private final Integer code;
    private final String desc;

    CouponValidTypeEnum(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
