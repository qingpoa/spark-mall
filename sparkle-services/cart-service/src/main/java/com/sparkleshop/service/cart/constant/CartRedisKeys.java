package com.sparkleshop.service.cart.constant;

public final class CartRedisKeys {

    public static final String CART = "cart:";

    private CartRedisKeys() {
    }

    public static String cart(Long userId) {
        return CART + userId;
    }
}
