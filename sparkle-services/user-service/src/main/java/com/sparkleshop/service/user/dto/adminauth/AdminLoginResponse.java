package com.sparkleshop.service.user.dto.adminauth;

import lombok.Data;

@Data
public class AdminLoginResponse {

    private Long adminId;

    private String token;

    private Long expiresIn;

    private AdminInfo adminInfo;

    @Data
    public static class AdminInfo {

        private Long id;

        private String username;

        private String nickname;

        private Integer level;
    }
}
