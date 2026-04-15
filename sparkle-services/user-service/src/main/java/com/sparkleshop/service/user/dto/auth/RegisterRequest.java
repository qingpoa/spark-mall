package com.sparkleshop.service.user.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterRequest {

    @NotBlank
    @Size(min = 4, max = 20)
    private String username;

    @NotBlank
    @Size(min = 6, max = 20)
    private String password;

    private String mobile;

    private String code;

    private String nickname;
}
