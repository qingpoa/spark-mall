package com.sparkleshop.service.user.dto.admin;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AdminUserStatusUpdateRequest {

    @NotNull
    private Integer status;
}
