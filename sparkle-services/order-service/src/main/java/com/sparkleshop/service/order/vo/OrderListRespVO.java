package com.sparkleshop.service.order.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class OrderListRespVO {

    private List<Item> list;

    private long total;

    private long pageNo;

    private long pageSize;

    @Data
    public static class Item {

        private Long orderId;

        private String orderNo;

        private Integer status;

        private BigDecimal actualAmount;

        private LocalDateTime createTime;

        private Integer itemCount;

        private List<OrderItem> items;
    }

    @Data
    public static class OrderItem {

        private Long skuId;

        private String skuName;

        private Integer quantity;

        private BigDecimal price;

        private BigDecimal totalPrice;
    }
}
