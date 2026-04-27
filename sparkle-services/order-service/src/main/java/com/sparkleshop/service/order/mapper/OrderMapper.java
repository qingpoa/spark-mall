package com.sparkleshop.service.order.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sparkleshop.service.order.entity.OrderDO;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDateTime;
import java.util.List;

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

    default OrderDO selectUserOrderById(Long userId, Long orderId) {
        return selectOne(new LambdaQueryWrapper<OrderDO>()
                .eq(OrderDO::getUserId, userId)
                .eq(OrderDO::getId, orderId)
                .last("limit 1"));
    }

    default List<OrderDO> selectExpiredPendingOrders(LocalDateTime now, int limit) {
        return selectList(new LambdaQueryWrapper<OrderDO>()
                .eq(OrderDO::getStatus, 1)
                .le(OrderDO::getExpireTime, now)
                .orderByAsc(OrderDO::getId)
                .last("limit " + limit));
    }

    default int updateUserOrderStatusIfMatch(Long userId,
                                             Long orderId,
                                             Integer expectedStatus,
                                             Integer targetStatus,
                                             LocalDateTime cancelTime,
                                             LocalDateTime updateTime) {
        return update(new LambdaUpdateWrapper<OrderDO>()
                .eq(OrderDO::getUserId, userId)
                .eq(OrderDO::getId, orderId)
                .eq(OrderDO::getStatus, expectedStatus)
                .set(OrderDO::getStatus, targetStatus)
                .set(OrderDO::getCancelTime, cancelTime)
                .set(OrderDO::getUpdateTime, updateTime));
    }

    default int updateUserOrderPaidIfMatch(Long userId,
                                           Long orderId,
                                           Integer expectedStatus,
                                           Integer targetStatus,
                                           LocalDateTime now,
                                           LocalDateTime payTime,
                                           LocalDateTime updateTime) {
        return update(new LambdaUpdateWrapper<OrderDO>()
                .eq(OrderDO::getUserId, userId)
                .eq(OrderDO::getId, orderId)
                .eq(OrderDO::getStatus, expectedStatus)
                .gt(OrderDO::getExpireTime, now)
                .set(OrderDO::getStatus, targetStatus)
                .set(OrderDO::getPayTime, payTime)
                .set(OrderDO::getUpdateTime, updateTime));
    }

    default int updateOrderClosedIfMatch(Long orderId,
                                         Integer expectedStatus,
                                         Integer targetStatus,
                                         LocalDateTime closeTime,
                                         LocalDateTime updateTime) {
        return update(new LambdaUpdateWrapper<OrderDO>()
                .eq(OrderDO::getId, orderId)
                .eq(OrderDO::getStatus, expectedStatus)
                .set(OrderDO::getStatus, targetStatus)
                .set(OrderDO::getCloseTime, closeTime)
                .set(OrderDO::getUpdateTime, updateTime));
    }
}
