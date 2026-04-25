package com.sparkleshop.service.order.constant;

public final class OrderRedisKeys {

    public static final String ORDER_SUBMIT_TOKEN = "order:submit:token:";
    public static final long ORDER_SUBMIT_TOKEN_EXPIRE_MINUTES = 15L;

    private OrderRedisKeys() {
    }

    public static String submitToken(Long userId) {
        return ORDER_SUBMIT_TOKEN + userId;
    }
}
