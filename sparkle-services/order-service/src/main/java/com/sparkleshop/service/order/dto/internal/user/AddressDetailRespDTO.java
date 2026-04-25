package com.sparkleshop.service.order.dto.internal.user;

import lombok.Data;

@Data
public class AddressDetailRespDTO {

    private Long id;

    private Long userId;

    private String receiverName;

    private String receiverMobile;

    private String province;

    private String city;

    private String district;

    private String detailAddress;
}
