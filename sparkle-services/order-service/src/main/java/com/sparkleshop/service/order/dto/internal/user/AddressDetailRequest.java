package com.sparkleshop.service.order.dto.internal.user;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AddressDetailRequest {

    @NotNull
    private Long userId;

    @NotNull
    private Long addressId;
}
