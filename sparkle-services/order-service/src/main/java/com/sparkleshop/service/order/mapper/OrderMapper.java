package com.sparkleshop.service.order.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sparkleshop.service.order.entity.OrderDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface OrderMapper extends BaseMapper<OrderDO> {
    default Page<OrderDO> selectUserOrderPage(Page<OrderDO> page, Long userId, Integer status) {
        LambdaQueryWrapper<OrderDO> queryWrapper = new LambdaQueryWrapper<OrderDO>()
                .eq(OrderDO::getUserId, userId)
                .orderByDesc(OrderDO::getCreateTime, OrderDO::getId);

        if (status != null) {
            queryWrapper.eq(OrderDO::getStatus, status);
        }

        return selectPage(page, queryWrapper);
    }
}
