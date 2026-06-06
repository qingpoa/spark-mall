package com.sparkleshop.service.user.service;

import com.sparkleshop.service.user.dto.adminauth.AdminChangePasswordRequest;
import com.sparkleshop.service.user.dto.adminauth.AdminCreateRequest;
import com.sparkleshop.service.user.dto.adminauth.AdminCreateResponse;
import com.sparkleshop.service.user.dto.adminauth.AdminLoginRequest;
import com.sparkleshop.service.user.dto.adminauth.AdminLoginResponse;

public interface AdminAuthService {

    AdminLoginResponse login(AdminLoginRequest request);

    AdminCreateResponse createAdmin(AdminCreateRequest request);

    void changePassword(AdminChangePasswordRequest request);
}
