package com.sparkleshop.service.order.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sparkleshop.service.order.entity.OrderItemDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

@Mapper
public interface OrderItemMapper extends BaseMapper<OrderItemDO> {

    default List<OrderItemDO> selectByOrderIds(Collection<Long> orderIds) {
        if (orderIds == null || orderIds.isEmpty()) {
            return Collections.emptyList();
        }

        LambdaQueryWrapper<OrderItemDO> queryWrapper = new LambdaQueryWrapper<OrderItemDO>()
                .in(OrderItemDO::getOrderId, orderIds)
                .orderByAsc(OrderItemDO::getOrderId, OrderItemDO::getId);

        return selectList(queryWrapper);
    }
}
