package com.sparkleshop.service.user.dto.profile;

import lombok.Data;

@Data
public class UserProfileUpdateRequest {

    private String nickname;

    private String email;
}
