package com.sparkleshop.service.coupon.constant;

public final class CouponRedisKeys {

    public static final String COUPON_RECEIVE_LOCK = "lock:coupon:receive:";
    public static final String COUPON_TEMPLATE_LOAD_LOCK = "lock:coupon:template:load:";
    public static final String COUPON_TEMPLATE = "coupon:template:";
    public static final long COUPON_TEMPLATE_TTL_MINUTES = 30;

    private CouponRedisKeys() {
    }

    public static String couponReceiveLock(Long userId, Long templateId) {
        return COUPON_RECEIVE_LOCK + userId + ":" + templateId;
    }

    public static String couponTemplateLoadLock(Long templateId) {
        return COUPON_TEMPLATE_LOAD_LOCK + templateId;
    }

    public static String couponTemplate(Long templateId) {
        return COUPON_TEMPLATE + templateId;
    }
}
