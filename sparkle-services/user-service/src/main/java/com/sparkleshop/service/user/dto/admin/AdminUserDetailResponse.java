package com.sparkleshop.service.user.dto.admin;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AdminUserDetailResponse {

    private Long id;

    private String username;

    private String nickname;

    private String mobile;

    private String email;

    private String avatar;

    private Integer status;

    private Integer level;

    private String loginIp;

    private LocalDateTime loginTime;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
