package com.sparkleshop.service.user.dto.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequest {

    private String username;

    private String password;

    private String mobile;

    private String code;

    @NotBlank
    private String loginType;
}
