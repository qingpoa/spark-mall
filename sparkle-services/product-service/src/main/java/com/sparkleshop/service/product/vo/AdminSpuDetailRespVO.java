package com.sparkleshop.service.product.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
public class AdminSpuDetailRespVO {

    private Long id;

    private String name;

    private Long categoryId;

    private Long brandId;

    private String description;

    private String mainImage;

    private List<String> images;

    private Integer salesCount;

    private Integer status;

    private Integer isHot;

    private Integer isNew;

    private List<SkuItem> skus;

    @Data
    public static class SkuItem {

        private Long skuId;

        private String name;

        private Map<String, Object> spec;

        private BigDecimal price;

        private BigDecimal costPrice;

        private BigDecimal weight;

        private String image;

        private Integer status;

        private Integer stock;

        private Integer lockedStock;

        private Integer availableStock;
    }
}
