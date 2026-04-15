package com.sparkleshop.service.user.service;

import com.sparkleshop.common.core.model.PageResponse;
import com.sparkleshop.service.user.dto.admin.AdminUserPageItemResponse;
import com.sparkleshop.service.user.dto.admin.AdminUserPageRequest;
import com.sparkleshop.service.user.dto.admin.AdminUserStatusUpdateRequest;

public interface AdminUserService {

    PageResponse<AdminUserPageItemResponse> getUserPage(AdminUserPageRequest request);

    void updateUserStatus(Long userId, AdminUserStatusUpdateRequest request);
}
