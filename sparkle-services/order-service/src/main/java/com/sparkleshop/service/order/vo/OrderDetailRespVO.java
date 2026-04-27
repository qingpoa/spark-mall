package com.sparkleshop.service.order.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class OrderDetailRespVO {

    private Long orderId;

    private String orderNo;

    private Integer status;

    private BigDecimal totalAmount;

    private BigDecimal discountAmount;

    private BigDecimal actualAmount;

    private Long couponId;

    private String receiverName;

    private String receiverMobile;

    private String receiverAddress;

    private LocalDateTime payTime;

    private LocalDateTime cancelTime;

    private LocalDateTime closeTime;

    private LocalDateTime completeTime;

    private LocalDateTime expireTime;

    private LocalDateTime createTime;

    private List<OrderItem> items;

    @Data
    public static class OrderItem {

        private Long skuId;

        private Long spuId;

        private String skuName;

        private String spuName;

        private Integer quantity;

        private BigDecimal price;

        private BigDecimal totalPrice;

        private String specJson;
    }
}
