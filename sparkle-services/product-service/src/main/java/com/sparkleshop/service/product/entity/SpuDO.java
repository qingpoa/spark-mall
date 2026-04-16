package com.sparkleshop.service.product.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("spu")
public class SpuDO {

    @TableId
    private Long id;

    private String name;

    private Long categoryId;

    private Long brandId;

    private String description;

    private String mainImage;

    private String images;

    private Integer salesCount;

    private Integer status;

    private Integer isHot;

    private Integer isNew;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
