package com.sparkleshop.service.user.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sparkleshop.service.user.entity.ShopUserDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ShopUserMapper extends BaseMapper<ShopUserDO> {

    default ShopUserDO selectByUsername(String username) {
        return selectOne(new LambdaQueryWrapper<ShopUserDO>().eq(ShopUserDO::getUsername, username));
    }

    default ShopUserDO selectByMobile(String mobile) {
        return selectOne(new LambdaQueryWrapper<ShopUserDO>().eq(ShopUserDO::getMobile, mobile));
    }
}
