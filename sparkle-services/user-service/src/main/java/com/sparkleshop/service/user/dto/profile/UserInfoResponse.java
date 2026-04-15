package com.sparkleshop.service.user.dto.profile;

import lombok.Data;

@Data
public class UserInfoResponse {

    private Long id;

    private String username;

    private String nickname;

    private String mobile;

    private String email;

    private String avatar;

    private Integer status;

    private Integer level;
}
