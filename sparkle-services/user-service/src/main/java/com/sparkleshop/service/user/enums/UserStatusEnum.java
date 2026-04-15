package com.sparkleshop.service.user.enums;

import lombok.Getter;

@Getter
public enum UserStatusEnum {

    DISABLED(0),
    ENABLED(1);

    private final int code;

    UserStatusEnum(int code) {
        this.code = code;
    }

    public static boolean isEnabled(Integer code) {
        return ENABLED.code == code;
    }
}
