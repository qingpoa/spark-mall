package com.sparkleshop.service.user.dto.admin;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AdminUserLevelUpdateRequest {

    @NotNull
    @Min(1)
    @Max(127)
    private Integer level;
}
