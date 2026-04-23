package com.sparkleshop.common.security.enums;

import lombok.Getter;

@Getter
public enum UserTypeEnum {

    MEMBER(1),
    ADMIN(2);

    private final int code;

    UserTypeEnum(int code) {
        this.code = code;
    }

}
