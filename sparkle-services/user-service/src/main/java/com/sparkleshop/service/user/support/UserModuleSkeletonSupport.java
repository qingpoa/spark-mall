package com.sparkleshop.service.user.support;

import com.sparkleshop.common.core.exception.BusinessException;

public final class UserModuleSkeletonSupport {

    public static final int NOT_IMPLEMENTED_CODE = 50100;

    private UserModuleSkeletonSupport() {
    }

    public static BusinessException notImplemented(String capability) {
        return new BusinessException(NOT_IMPLEMENTED_CODE, capability + " is not implemented yet.");
    }
}
