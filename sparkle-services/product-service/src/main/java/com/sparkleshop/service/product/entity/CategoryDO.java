package com.sparkleshop.service.product.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("category")
public class CategoryDO {

    @TableId
    private Long id;

    private String name;

    private Long parentId;

    private Integer level;

    private Integer sort;

    private Integer status;

    private String icon;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
