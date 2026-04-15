package com.sparkleshop.service.user.dto.auth;

import lombok.Data;

@Data
public class LoginResponse {

    private Long userId;

    private String token;

    private Long expiresIn;

    private UserInfo userInfo;

    @Data
    public static class UserInfo {

        private Long id;

        private String username;

        private String nickname;
    }
}
