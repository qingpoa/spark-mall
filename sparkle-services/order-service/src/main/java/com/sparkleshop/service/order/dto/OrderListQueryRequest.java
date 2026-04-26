package com.sparkleshop.service.order.dto;

import lombok.Data;

@Data
public class OrderListQueryRequest {

    private long pageNo = 1;

    private long pageSize = 10;

    private Integer status;
}
