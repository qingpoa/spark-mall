package com.sparkleshop.service.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sparkleshop.service.order.entity.OrderOperateLogDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface OrderOperateLogMapper extends BaseMapper<OrderOperateLogDO> {
}
