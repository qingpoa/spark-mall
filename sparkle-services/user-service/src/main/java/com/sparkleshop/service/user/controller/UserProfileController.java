package com.sparkleshop.service.user.controller;

import com.sparkleshop.common.core.model.Result;
import com.sparkleshop.common.security.annotation.RequireLogin;
import com.sparkleshop.common.web.util.Results;
import com.sparkleshop.service.user.dto.profile.ChangePasswordRequest;
import com.sparkleshop.service.user.dto.profile.UserProfileUpdateRequest;
import com.sparkleshop.service.user.service.UserProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/user")
public class UserProfileController {

    private final UserProfileService userProfileService;

    @RequireLogin
    @GetMapping("/info")
    public ResponseEntity<Result> getCurrentUser() {
        return Results.ok(userProfileService.getCurrentUser());
    }

    @RequireLogin
    @PutMapping("/info")
    public ResponseEntity<Result> updateCurrentUser(@RequestBody UserProfileUpdateRequest request) {
        userProfileService.updateCurrentUser(request);
        return Results.ok();
    }

    @RequireLogin
    @PutMapping("/password")
    public ResponseEntity<Result> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        userProfileService.changePassword(request);
        return Results.ok();
    }

    @RequireLogin
    @PostMapping("/avatar")
    public ResponseEntity<Result> uploadAvatar(@RequestParam("file") MultipartFile file) {
        return Results.ok(userProfileService.uploadAvatar(file));
    }

}
