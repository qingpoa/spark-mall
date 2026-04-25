package com.sparkleshop.service.order.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("shop_order")
public class OrderDO {

    @TableId
    private Long id;

    private String orderNo;

    private Long userId;

    private Long addressId;

    private String receiverName;

    private String receiverMobile;

    private String receiverAddress;

    private BigDecimal totalAmount;

    private BigDecimal actualAmount;

    private BigDecimal discountAmount;

    private Long couponId;

    private Integer status;

    private LocalDateTime payTime;

    private LocalDateTime cancelTime;

    private LocalDateTime closeTime;

    private LocalDateTime completeTime;

    private LocalDateTime expireTime;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
