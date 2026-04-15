package com.sparkleshop.service.user.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("shop_user")
public class ShopUserDO {

    @TableId
    private Long id;

    private String username;

    private String password;

    private String nickname;

    private String mobile;

    private String email;

    private String avatar;

    private Integer status;

    private Integer level;

    private String loginIp;

    private LocalDateTime loginTime;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
