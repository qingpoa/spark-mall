package com.sparkleshop.service.user.controller;

import com.sparkleshop.common.core.model.Result;
import com.sparkleshop.common.web.util.Results;
import com.sparkleshop.service.user.dto.adminauth.AdminChangePasswordRequest;
import com.sparkleshop.service.user.dto.adminauth.AdminCreateRequest;
import com.sparkleshop.service.user.dto.adminauth.AdminLoginRequest;
import com.sparkleshop.service.user.service.AdminAuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/admin/auth")
public class AdminAuthController {

    private final AdminAuthService adminAuthService;

    @PostMapping("/register")
    public ResponseEntity<Result> createAdmin(@Valid @RequestBody AdminCreateRequest request) {
        return Results.created(adminAuthService.createAdmin(request));
    }

    @PostMapping("/login")
    public ResponseEntity<Result> login(@Valid @RequestBody AdminLoginRequest request) {
        return Results.ok(adminAuthService.login(request));
    }

    @PutMapping("/password")
    public ResponseEntity<Result> changePassword(@Valid @RequestBody AdminChangePasswordRequest request) {
        adminAuthService.changePassword(request);
        return Results.ok();
    }
}
