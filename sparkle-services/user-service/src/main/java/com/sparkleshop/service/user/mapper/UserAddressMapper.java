package com.sparkleshop.service.user.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sparkleshop.service.user.entity.UserAddressDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface UserAddressMapper extends BaseMapper<UserAddressDO> {

    default List<UserAddressDO> selectByUserId(Long userId) {
        return selectList(new LambdaQueryWrapper<UserAddressDO>()
                .eq(UserAddressDO::getUserId, userId)
                .orderByDesc(UserAddressDO::getIsDefault)
                .orderByDesc(UserAddressDO::getId));
    }

    default UserAddressDO selectByIdAndUserId(Long addressId, Long userId) {
        return selectOne(new LambdaQueryWrapper<UserAddressDO>()
                .eq(UserAddressDO::getId, addressId)
                .eq(UserAddressDO::getUserId, userId));
    }

    default void clearDefaultByUserId(Long userId) {
        update(null, new LambdaUpdateWrapper<UserAddressDO>()
                .eq(UserAddressDO::getUserId, userId)
                .eq(UserAddressDO::getIsDefault, 1)
                .set(UserAddressDO::getIsDefault, 0));
    }
}
