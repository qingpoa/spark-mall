package com.sparkleshop.service.order.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SubmitOrderRespVO {

    private Long orderId;

    private String orderNo;

    private Integer status;

    private LocalDateTime expireTime;
}
