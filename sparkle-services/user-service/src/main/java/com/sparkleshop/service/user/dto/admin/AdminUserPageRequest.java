package com.sparkleshop.service.user.dto.admin;

import lombok.Data;

@Data
public class AdminUserPageRequest {

    private long pageNo = 1;

    private long pageSize = 10;

    private String username;

    private String mobile;

    private Integer status;
}
