package com.sparkleshop.service.user.dto.address;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AddressDetailRequest {

    @NotNull(message = "userId 不能为空")
    private Long userId;

    @NotNull(message = "addressId 不能为空")
    private Long addressId;
}
