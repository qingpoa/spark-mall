package com.sparkleshop.service.product.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ProductCategoryTreeRespVO {

    private Long id;

    private String name;

    private Long parentId;

    private Integer level;

    private Integer sort;

    private String icon;

    private List<ProductCategoryTreeRespVO> children = new ArrayList<>();
}
