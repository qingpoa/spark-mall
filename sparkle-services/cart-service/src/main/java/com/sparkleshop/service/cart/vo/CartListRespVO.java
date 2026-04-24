package com.sparkleshop.service.cart.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class CartListRespVO {

    private List<CartItemRespVO> items;

    private BigDecimal totalAmount;

    private BigDecimal selectedAmount;

    private Integer selectedCount;
}
