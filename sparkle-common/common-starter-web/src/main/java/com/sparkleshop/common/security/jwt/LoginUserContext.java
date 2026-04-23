package com.sparkleshop.common.security.jwt;

import com.sparkleshop.common.core.exception.BusinessException;
import com.sparkleshop.common.security.constant.SecurityErrorCodes;

public final class LoginUserContext {

    private static final ThreadLocal<TokenUser> HOLDER = new ThreadLocal<>();

    private LoginUserContext() {
    }

    public static void set(TokenUser tokenUser) {
        HOLDER.set(tokenUser);
    }

    public static TokenUser get() {
        return HOLDER.get();
    }

    public static TokenUser getRequired() {
        TokenUser tokenUser = HOLDER.get();
        if (tokenUser == null) {
            throw new BusinessException(SecurityErrorCodes.UNAUTHORIZED, "未登录或登录态无效");
        }
        return tokenUser;
    }

    public static Long getRequiredUserId() {
        return getRequired().getUserId();
    }

    public static void clear() {
        HOLDER.remove();
    }
}
