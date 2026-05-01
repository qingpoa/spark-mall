package com.sparkleshop.service.coupon.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class CouponListRespVO {

    private List<Item> list;

    private long total;

    private long pageNo;

    private long pageSize;

    @Data
    public static class Item {

        private Long templateId;

        private String name;

        private Integer type;

        private BigDecimal amount;

        private BigDecimal minAmount;

        private Integer validType;

        private Integer validDays;

        private LocalDateTime startTime;

        private LocalDateTime endTime;

        private Integer receiveLimit;

        private long receivedCount;
    }
}
