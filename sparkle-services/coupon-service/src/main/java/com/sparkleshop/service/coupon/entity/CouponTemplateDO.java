package com.sparkleshop.service.coupon.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("coupon_template")
public class CouponTemplateDO {

    @TableId
    private Long id;

    private String name;

    private Integer type;

    private BigDecimal amount;

    private BigDecimal minAmount;

    private Integer totalCount;

    private Integer issuedCount;

    private Integer receiveLimit;

    private Integer validType;

    private Integer validDays;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private Integer status;

    private String description;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
