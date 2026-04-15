package com.sparkleshop.common.redis.key;

public final class RedisKeys {

    public static final String JWT_BLACKLIST = "jwt:blacklist:";
    public static final String USER_INFO = "user:info:";
    public static final String USER_ADDRESS_DEFAULT_LOCK = "lock:user:address:default:";
    public static final String CART = "sparkle:cart:";
    public static final String PRODUCT_CACHE = "sparkle:product:";
    public static final String COUPON = "sparkle:coupon:";

    private RedisKeys() {
    }
}
