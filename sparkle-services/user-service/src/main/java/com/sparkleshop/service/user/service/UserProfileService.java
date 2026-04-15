package com.sparkleshop.service.user.service;

import com.sparkleshop.service.user.dto.profile.ChangePasswordRequest;
import com.sparkleshop.service.user.dto.profile.UserInfoResponse;
import com.sparkleshop.service.user.dto.profile.UserProfileUpdateRequest;

public interface UserProfileService {

    UserInfoResponse getCurrentUser();

    void updateCurrentUser(UserProfileUpdateRequest request);

    void changePassword(ChangePasswordRequest request);
}
