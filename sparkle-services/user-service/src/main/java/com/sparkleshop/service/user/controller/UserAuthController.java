package com.sparkleshop.service.user.controller;

import com.sparkleshop.common.core.model.Result;
import com.sparkleshop.common.security.annotation.RequireLogin;
import com.sparkleshop.common.web.util.Results;
import com.sparkleshop.service.user.dto.auth.LoginRequest;
import com.sparkleshop.service.user.dto.auth.RegisterRequest;
import com.sparkleshop.service.user.service.UserAuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/user")
public class UserAuthController {

    private final UserAuthService userAuthService;

    @PostMapping("/register")
    public ResponseEntity<Result> register(@Valid @RequestBody RegisterRequest request) {
        return Results.created(userAuthService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<Result> login(@Valid @RequestBody LoginRequest request) {
        return Results.ok(userAuthService.login(request));
    }

    @RequireLogin
    @PostMapping("/logout")
    public ResponseEntity<Result> logout() {
        userAuthService.logout();
        return Results.ok();
    }
}
