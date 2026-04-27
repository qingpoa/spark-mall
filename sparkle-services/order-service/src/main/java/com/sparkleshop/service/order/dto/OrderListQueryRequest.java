package com.sparkleshop.service.order.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class OrderListQueryRequest {

    @Min(1)
    private long pageNo = 1;

    @Min(1)
    @Max(100)
    private long pageSize = 10;

    private Integer status;
}
