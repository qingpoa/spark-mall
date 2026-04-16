package com.sparkleshop.service.product.mapper;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sparkleshop.service.product.entity.BrandDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface BrandMapper extends BaseMapper<BrandDO> {

    default List<BrandDO> selectEnabledList(String keyword) {
        LambdaQueryWrapper<BrandDO> wrapper = new LambdaQueryWrapper<BrandDO>()
                .eq(BrandDO::getStatus, 1)
                .orderByAsc(BrandDO::getSort, BrandDO::getId);
        if (StrUtil.isNotBlank(keyword)) {
            wrapper.like(BrandDO::getName, StrUtil.trim(keyword));
        }
        return selectList(wrapper);
    }
}
