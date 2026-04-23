package com.sparkleshop.common.security.constant;

public final class SecurityConstants {

    public static final String AUTHORIZATION_HEADER = "Authorization";
    public static final String BEARER_PREFIX = "Bearer ";
    public static final String USER_ID_HEADER = "X-User-Id";
    public static final String USER_TYPE_HEADER = "X-User-Type";
    public static final String TOKEN_ID_HEADER = "X-Token-Id";
    public static final String CLAIM_USER_ID = "userId";
    public static final String CLAIM_USER_TYPE = "userType";
    public static final String CLAIM_TOKEN_ID = "jti";

    private SecurityConstants() {
    }
}
