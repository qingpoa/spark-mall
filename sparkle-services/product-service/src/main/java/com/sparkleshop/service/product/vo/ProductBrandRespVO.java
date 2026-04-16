package com.sparkleshop.service.product.vo;

import lombok.Data;

@Data
public class ProductBrandRespVO {

    private Long id;

    private String name;

    private String logo;

    private String description;

    private Integer sort;
}
