package com.sparkleshop.service.user.constant;

public final class UserErrorCodes {

    public static final int INVALID_REQUEST = 40000;
    public static final int LOGIN_TYPE_NOT_SUPPORTED = 40102;
    public static final int BAD_CREDENTIALS = 40104;
    public static final int USERNAME_ALREADY_EXISTS = 40901;
    public static final int MOBILE_ALREADY_EXISTS = 40902;
    public static final int ADDRESS_NOT_FOUND = 40903;
    public static final int OLD_PASSWORD_INCORRECT = 40907;
    public static final int LOGIN_TYPE_NOT_ENABLED = 40908;
    public static final int USER_DISABLED = 40909;

    private UserErrorCodes() {
    }
}
