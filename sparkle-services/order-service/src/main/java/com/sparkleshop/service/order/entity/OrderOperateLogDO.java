package com.sparkleshop.service.order.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("order_operate_log")
public class OrderOperateLogDO {

    @TableId
    private Long id;

    private Long orderId;

    private Integer operatorType;

    private Long operatorId;

    private String action;

    private Integer beforeStatus;

    private Integer afterStatus;

    private String remark;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
