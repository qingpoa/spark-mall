package com.sparkleshop.service.user.service;

import com.sparkleshop.service.user.dto.profile.AvatarResponse;
import com.sparkleshop.service.user.dto.profile.ChangePasswordRequest;
import com.sparkleshop.service.user.dto.profile.UserInfoResponse;
import com.sparkleshop.service.user.dto.profile.UserProfileUpdateRequest;
import org.springframework.web.multipart.MultipartFile;

public interface UserProfileService {

    UserInfoResponse getCurrentUser();

    void updateCurrentUser(UserProfileUpdateRequest request);

    void changePassword(ChangePasswordRequest request);

    AvatarResponse uploadAvatar(MultipartFile file);
}
