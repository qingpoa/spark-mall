package com.sparkleshop.service.user.dto.address;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AddressCreateRequest {

    @NotBlank
    private String receiverName;

    @NotBlank
    private String receiverMobile;

    @NotBlank
    private String province;

    @NotBlank
    private String city;

    @NotBlank
    private String district;

    @NotBlank
    private String detailAddress;

    private Integer isDefault;
}
