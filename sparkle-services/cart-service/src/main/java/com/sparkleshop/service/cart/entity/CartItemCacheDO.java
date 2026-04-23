package com.sparkleshop.service.cart.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CartItemCacheDO {

    private Integer quantity;

    private Boolean selected;

    private LocalDateTime addTime;
}
