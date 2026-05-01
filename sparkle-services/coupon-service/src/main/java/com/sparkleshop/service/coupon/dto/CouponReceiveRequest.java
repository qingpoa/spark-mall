package com.sparkleshop.service.coupon.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CouponReceiveRequest {

    @NotNull(message = "templateId 不能为空")
    private Long templateId;
}
