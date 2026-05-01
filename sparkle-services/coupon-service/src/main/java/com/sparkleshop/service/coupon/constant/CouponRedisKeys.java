package com.sparkleshop.service.coupon.constant;

public final class CouponRedisKeys {

    public static final String COUPON_RECEIVE_LOCK = "lock:coupon:receive:";

    private CouponRedisKeys() {
    }

    public static String couponReceiveLock(Long userId, Long templateId) {
        return COUPON_RECEIVE_LOCK + userId + ":" + templateId;
    }
}
