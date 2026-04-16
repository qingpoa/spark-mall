package com.sparkleshop.service.product.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sparkleshop.service.product.entity.SkuStockDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

@Mapper
public interface SkuStockMapper extends BaseMapper<SkuStockDO> {

    default List<SkuStockDO> selectBySkuIds(Collection<Long> skuIds) {
        if (skuIds == null || skuIds.isEmpty()) {
            return Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapper<SkuStockDO>()
                .in(SkuStockDO::getSkuId, skuIds));
    }

    default SkuStockDO selectBySkuId(Long skuId) {
        return selectOne(new LambdaQueryWrapper<SkuStockDO>()
                .eq(SkuStockDO::getSkuId, skuId));
    }
}
