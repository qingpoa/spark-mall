package com.sparkleshop.service.order.constant;

public final class OrderErrorCodes {

    public static final int INVALID_REQUEST = 40000;
    public static final int RESOURCE_NOT_FOUND = 40400;
    public static final int ORDER_NOT_FOUND = 40401;
    public static final int REPEAT_SUBMIT = 40900;
    public static final int ADDRESS_NOT_FOUND = 40903;
    public static final int PRODUCT_STOCK_INSUFFICIENT = 40904;
    public static final int COUPON_UNAVAILABLE = 40905;
    public static final int ORDER_STATUS_INVALID = 40906;

    private OrderErrorCodes() {
    }
}
