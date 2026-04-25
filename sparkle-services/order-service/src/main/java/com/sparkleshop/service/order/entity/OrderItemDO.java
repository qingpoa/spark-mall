package com.sparkleshop.service.order.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("order_item")
public class OrderItemDO {

    @TableId
    private Long id;

    private Long orderId;

    private Long skuId;

    private Long spuId;

    private String skuName;

    private String spuName;

    private Integer quantity;

    private BigDecimal price;

    private BigDecimal totalPrice;

    private String specJson;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
