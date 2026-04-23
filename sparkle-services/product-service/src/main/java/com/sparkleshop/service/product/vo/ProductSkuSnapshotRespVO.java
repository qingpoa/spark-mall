package com.sparkleshop.service.product.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Map;

@Data
public class ProductSkuSnapshotRespVO {

    private Long skuId;

    private Long spuId;

    private String skuName;

    private String spuName;

    private String image;

    private Map<String, Object> spec;

    private BigDecimal price;

    private Integer stock;

    private Boolean available;
}
