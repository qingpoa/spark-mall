package com.sparkleshop.service.user.dto.admin;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AdminUserPageItemResponse {

    private Long id;

    private String username;

    private String nickname;

    private String mobile;

    private Integer status;

    private Integer level;

    private LocalDateTime createTime;
}
