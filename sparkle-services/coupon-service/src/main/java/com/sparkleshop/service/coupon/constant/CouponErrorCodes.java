package com.sparkleshop.service.coupon.constant;

public final class CouponErrorCodes {

    public static final int INVALID_REQUEST = 40000;
    public static final int RESOURCE_NOT_FOUND = 40400;
    public static final int COUPON_NOT_FOUND = 40401;
    public static final int COUPON_TEMPLATE_NOT_FOUND = 40402;
    public static final int COUPON_UNAVAILABLE = 40905;
    public static final int COUPON_STATUS_INVALID = 40906;
    public static final int COUPON_RECEIVE_LIMIT_EXCEEDED = 40907;
    public static final int COUPON_ISSUED_OUT = 40908;

    private CouponErrorCodes() {
    }
}
