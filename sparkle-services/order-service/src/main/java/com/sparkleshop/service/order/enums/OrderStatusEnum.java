package com.sparkleshop.service.order.enums;

import lombok.Getter;

@Getter
public enum OrderStatusEnum {

    PENDING_PAYMENT(1, "待支付"),
    PAID(2, "已支付"),
    DELIVERED(3, "已发货"),
    COMPLETED(4, "已完成"),
    CANCELED(5, "已取消"),
    CLOSED(6, "已关闭");

    private final Integer code;
    private final String desc;

    OrderStatusEnum(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
