package com.sparkleshop.service.user.mapper;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
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

    default Page<ShopUserDO> selectAdminPage(Page<ShopUserDO> page, String username, String mobile, Integer status) {
        LambdaQueryWrapper<ShopUserDO> queryWrapper = new LambdaQueryWrapper<ShopUserDO>()
                .like(StrUtil.isNotBlank(username), ShopUserDO::getUsername, StrUtil.trim(username))
                .like(StrUtil.isNotBlank(mobile), ShopUserDO::getMobile, StrUtil.trim(mobile))
                .eq(status != null, ShopUserDO::getStatus, status)
                .orderByDesc(ShopUserDO::getCreateTime, ShopUserDO::getId);
        return selectPage(page, queryWrapper);
    }
}
