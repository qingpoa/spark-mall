package com.sparkleshop.service.user.service.impl;

import com.sparkleshop.common.core.model.PageResponse;
import com.sparkleshop.service.user.dto.admin.AdminUserPageItemResponse;
import com.sparkleshop.service.user.dto.admin.AdminUserPageRequest;
import com.sparkleshop.service.user.dto.admin.AdminUserStatusUpdateRequest;
import com.sparkleshop.service.user.service.AdminUserService;
import com.sparkleshop.service.user.support.UserModuleSkeletonSupport;
import org.springframework.stereotype.Service;

@Service
public class AdminUserServiceImpl implements AdminUserService {

    @Override
    public PageResponse<AdminUserPageItemResponse> getUserPage(AdminUserPageRequest request) {
        throw UserModuleSkeletonSupport.notImplemented("Admin user page");
    }

    @Override
    public void updateUserStatus(Long userId, AdminUserStatusUpdateRequest request) {
        throw UserModuleSkeletonSupport.notImplemented("Admin update user status");
    }
}
