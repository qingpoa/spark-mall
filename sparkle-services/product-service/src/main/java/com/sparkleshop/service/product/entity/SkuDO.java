package com.sparkleshop.service.product.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("sku")
public class SkuDO {

    @TableId
    private Long id;

    private Long spuId;

    private String name;

    private String specJson;

    private BigDecimal price;

    private BigDecimal costPrice;

    private BigDecimal weight;

    private String image;

    private Integer status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
