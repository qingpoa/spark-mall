package com.sparkleshop.service.user.constant;

public final class UserRedisKeys {

    public static final String USER_INFO = "user:info:";
    public static final String USER_ADDRESS_DEFAULT_LOCK = "lock:user:address:default:";

    private UserRedisKeys() {
    }

    public static String userInfo(Long userId) {
        return USER_INFO + userId;
    }

    public static String userAddressDefaultLock(Long userId) {
        return USER_ADDRESS_DEFAULT_LOCK + userId;
    }
}
