package com.sparkleshop.service.order.constant;

public final class OrderRedisKeys {

    public static final String ORDER_SUBMIT_TOKEN = "order:submit:token:";
    public static final long ORDER_SUBMIT_TOKEN_EXPIRE_MINUTES = 15L;
    public static final String ORDER_PAY_TOKEN = "order:pay:token:";
    public static final long ORDER_PAY_TOKEN_EXPIRE_MINUTES = 15L;

    private OrderRedisKeys() {
    }

    public static String submitToken(Long userId) {
        return ORDER_SUBMIT_TOKEN + userId;
    }

    public static String payToken(Long userId, Long orderId) {
        return ORDER_PAY_TOKEN + userId + ":" + orderId;
    }
}
