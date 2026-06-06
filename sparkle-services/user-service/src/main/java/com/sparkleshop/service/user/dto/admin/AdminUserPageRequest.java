package com.sparkleshop.service.user.dto.admin;

import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class AdminUserPageRequest {

    @Min(1)
    private long pageNo = 1;

    @Min(1)
    private long pageSize = 10;

    private String username;

    private String mobile;

    private Integer status;
}
