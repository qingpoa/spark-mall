package com.sparkleshop.service.product.vo;

import lombok.Data;

@Data
public class AdminCategoryRespVO {

    private Long id;

    private String name;

    private Long parentId;

    private Integer level;

    private Integer sort;

    private Integer status;

    private String icon;
}
