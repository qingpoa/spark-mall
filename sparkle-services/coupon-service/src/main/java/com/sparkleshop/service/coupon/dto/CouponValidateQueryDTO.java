package com.sparkleshop.service.coupon.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CouponValidateQueryDTO {

    @NotNull(message = "orderAmount 不能为空")
    @DecimalMin(value = "0.01", message = "orderAmount 必须大于 0")
    private BigDecimal orderAmount;
}
