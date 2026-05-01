package com.sparkleshop.service.coupon.enums;

import lombok.Getter;

@Getter
public enum CouponStatusEnum {

    UNUSED(1, "未使用"),
    USED(2, "已使用"),
    EXPIRED(3, "已过期"),
    INVALID(4, "已失效"),
    OCCUPIED(5, "已占用");

    private final Integer code;
    private final String desc;

    CouponStatusEnum(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static boolean isUnused(Integer code) {
        return UNUSED.code.equals(code);
    }

    public static boolean isUsed(Integer code) {
        return USED.code.equals(code);
    }

    public static boolean isExpired(Integer code) {
        return EXPIRED.code.equals(code);
    }

    public static boolean isOccupied(Integer code) {
        return OCCUPIED.code.equals(code);
    }
}
