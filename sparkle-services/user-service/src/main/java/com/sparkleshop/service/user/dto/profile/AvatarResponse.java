package com.sparkleshop.service.user.dto.profile;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AvatarResponse {
    private Long userId;
    private String avatar;
}
