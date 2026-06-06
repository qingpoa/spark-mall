package com.sparkleshop.service.user.dto.adminauth;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AdminCreateRequest {

    @NotBlank
    private String username;

    @NotBlank
    private String password;

    private String nickname;

    @Min(1)
    @Max(4)
    private Integer level = 2;
}
