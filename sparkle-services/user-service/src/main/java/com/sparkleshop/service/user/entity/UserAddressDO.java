package com.sparkleshop.service.user.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("user_address")
public class UserAddressDO {

    @TableId
    private Long id;

    private Long userId;

    private String receiverName;

    private String receiverMobile;

    private String province;

    private String city;

    private String district;

    private String detailAddress;

    private Integer isDefault;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
