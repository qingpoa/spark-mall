package com.sparkleshop.service.coupon.enums;

import lombok.Getter;

@Getter
public enum CouponTypeEnum {

    FULL_REDUCTION(1, "满减券"),
    DISCOUNT(2, "折扣券"),
    NO_THRESHOLD(3, "无门槛券");

    private final Integer code;
    private final String desc;

    CouponTypeEnum(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static CouponTypeEnum fromCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (CouponTypeEnum type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        return null;
    }
}
