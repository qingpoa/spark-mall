package com.sparkleshop.service.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sparkleshop.service.order.entity.OrderDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface OrderMapper extends BaseMapper<OrderDO> {
}
