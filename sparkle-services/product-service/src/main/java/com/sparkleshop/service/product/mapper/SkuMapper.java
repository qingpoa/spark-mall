package com.sparkleshop.service.product.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sparkleshop.service.product.entity.SkuDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

@Mapper
public interface SkuMapper extends BaseMapper<SkuDO> {

    default List<SkuDO> selectBySpuIds(Collection<Long> spuIds) {
        if (spuIds == null || spuIds.isEmpty()) {
            return Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapper<SkuDO>()
                .in(SkuDO::getSpuId, spuIds)
                .eq(SkuDO::getStatus, 1)
                .orderByAsc(SkuDO::getPrice, SkuDO::getId));
    }

    default List<SkuDO> selectAdminBySpuIds(Collection<Long> spuIds) {
        if (spuIds == null || spuIds.isEmpty()) {
            return Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapper<SkuDO>()
                .in(SkuDO::getSpuId, spuIds)
                .orderByAsc(SkuDO::getId));
    }

    default List<SkuDO> selectBySpuId(Long spuId) {
        return selectList(new LambdaQueryWrapper<SkuDO>()
                .eq(SkuDO::getSpuId, spuId)
                .eq(SkuDO::getStatus, 1)
                .orderByAsc(SkuDO::getPrice, SkuDO::getId));
    }

}
