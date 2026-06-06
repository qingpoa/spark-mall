package com.sparkleshop.common.security.constant;

public final class SecurityRedisKeys {

    public static final String JWT_BLACKLIST = "jwt:blacklist:";
    public static final String AUTH_USER_LOGOUT_TIME = "auth:user_logout_time:";

    private SecurityRedisKeys() {
    }

    public static String jwtBlacklist(String tokenId) {
        return JWT_BLACKLIST + tokenId;
    }

    public static String authUserLogoutTime(Integer userType, Long userId) {
        return AUTH_USER_LOGOUT_TIME + userType + ":" + userId;
    }
}
