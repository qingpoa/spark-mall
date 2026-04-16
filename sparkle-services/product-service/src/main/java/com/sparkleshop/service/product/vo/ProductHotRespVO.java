package com.sparkleshop.service.product.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ProductHotRespVO {

    private Long skuId;

    private Long spuId;

    private String name;

    private BigDecimal price;

    private String mainImage;

    private Integer salesCount;

    private String tag;
}
