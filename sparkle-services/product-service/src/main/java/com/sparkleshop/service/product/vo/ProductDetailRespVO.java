package com.sparkleshop.service.product.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
public class ProductDetailRespVO {

    private Long skuId;

    private Long spuId;

    private String name;

    private BigDecimal price;

    private Integer stock;

    private String mainImage;

    private List<String> images;

    private Map<String, Object> spec;

    private Integer status;

    private String description;

    private Long categoryId;

    private Long brandId;

    private String brandName;

    private Integer salesCount;

    private Integer isHot;

    private Integer isNew;

    private List<SkuOption> skuOptions;

    @Data
    public static class SkuOption {

        private Long skuId;

        private String name;

        private BigDecimal price;

        private String image;
    }
}
