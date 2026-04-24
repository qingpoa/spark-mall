package com.sparkleshop.service.cart.vo;

import lombok.Data;

@Data
public class CartAddRespVO {

    private Long skuId;

    private Integer quantity;

    private Boolean selected;
}
