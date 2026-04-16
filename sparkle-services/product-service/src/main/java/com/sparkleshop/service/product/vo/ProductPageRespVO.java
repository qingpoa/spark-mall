package com.sparkleshop.service.product.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class ProductPageRespVO {

    private List<Item> list;

    private long total;

    private long pageNo;

    private long pageSize;

    @Data
    public static class Item {

        private Long skuId;

        private Long spuId;

        private String name;

        private BigDecimal price;

        private String mainImage;

        private Integer salesCount;

        private Integer stockStatus;

        private String specSummary;

        private Integer isHot;

        private Integer isNew;
    }
}
