package com.sparkleshop.service.product.vo;

import lombok.Data;

import java.util.List;

@Data
public class AdminSpuPageRespVO {

    private List<Item> list;

    private long total;

    private long pageNo;

    private long pageSize;

    @Data
    public static class Item {

        private Long id;

        private String name;

        private Long categoryId;

        private Long brandId;

        private String mainImage;

        private Integer salesCount;

        private Integer status;

        private Integer isHot;

        private Integer isNew;

        private Integer skuCount;
    }
}
