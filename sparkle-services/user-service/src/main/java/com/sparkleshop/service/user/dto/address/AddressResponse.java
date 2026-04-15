package com.sparkleshop.service.user.dto.address;

import lombok.Data;

@Data
public class AddressResponse {

    private Long id;

    private String receiverName;

    private String receiverMobile;

    private String province;

    private String city;

    private String district;

    private String detailAddress;

    private Integer isDefault;
}
