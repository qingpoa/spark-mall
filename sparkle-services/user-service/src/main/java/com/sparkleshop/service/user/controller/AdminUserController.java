package com.sparkleshop.service.user.controller;

import com.sparkleshop.common.core.model.Result;
import com.sparkleshop.common.security.annotation.RequireLogin;
import com.sparkleshop.common.web.util.Results;
import com.sparkleshop.service.user.dto.admin.AdminUserPageRequest;
import com.sparkleshop.service.user.dto.admin.AdminUserStatusUpdateRequest;
import com.sparkleshop.service.user.service.AdminUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/admin/user")
public class AdminUserController {

    private final AdminUserService adminUserService;

    @RequireLogin
    @GetMapping("/list")
    public ResponseEntity<Result> getUserPage(AdminUserPageRequest request) {
        return Results.ok(adminUserService.getUserPage(request));
    }

    @RequireLogin
    @PutMapping("/{userId}/status")
    public ResponseEntity<Result> updateUserStatus(@PathVariable("userId") Long userId,
                                                   @Valid @RequestBody AdminUserStatusUpdateRequest request) {
        adminUserService.updateUserStatus(userId, request);
        return Results.ok();
    }
}
