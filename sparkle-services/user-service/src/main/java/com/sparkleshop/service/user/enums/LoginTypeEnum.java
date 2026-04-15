package com.sparkleshop.service.user.enums;

import lombok.Getter;

@Getter
public enum LoginTypeEnum {

    PASSWORD("password"),
    CODE("code");

    private final String code;

    LoginTypeEnum(String code) {
        this.code = code;
    }

    public static LoginTypeEnum fromCode(String code) {
        for (LoginTypeEnum value : values()) {
            if (value.code.equalsIgnoreCase(code)) {
                return value;
            }
        }
        return null;
    }
}
