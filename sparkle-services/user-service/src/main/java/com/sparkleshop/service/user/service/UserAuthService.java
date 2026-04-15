package com.sparkleshop.service.user.service;

import com.sparkleshop.service.user.dto.auth.LoginRequest;
import com.sparkleshop.service.user.dto.auth.LoginResponse;
import com.sparkleshop.service.user.dto.auth.RegisterRequest;

public interface UserAuthService {

    LoginResponse register(RegisterRequest request);

    LoginResponse login(LoginRequest request);

    void logout();
}
