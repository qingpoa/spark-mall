package com.sparkleshop.service.product.mapper;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sparkleshop.service.product.entity.SpuDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.List;

@Mapper
public interface SpuMapper extends BaseMapper<SpuDO> {

    default List<SpuDO> selectActiveList(Collection<Long> categoryIds, Long brandId, String keyword) {
        LambdaQueryWrapper<SpuDO> wrapper = new LambdaQueryWrapper<SpuDO>()
                .eq(SpuDO::getStatus, 1);
        if (categoryIds != null && !categoryIds.isEmpty()) {
            wrapper.in(SpuDO::getCategoryId, categoryIds);
        }
        if (brandId != null) {
            wrapper.eq(SpuDO::getBrandId, brandId);
        }
        if (StrUtil.isNotBlank(keyword)) {
            wrapper.like(SpuDO::getName, StrUtil.trim(keyword));
        }
        return selectList(wrapper);
    }

    default Page<SpuDO> selectAdminPage(Page<SpuDO> page, String keyword, Long categoryId, Long brandId, Integer status) {
        LambdaQueryWrapper<SpuDO> wrapper = new LambdaQueryWrapper<SpuDO>()
                .orderByDesc(SpuDO::getCreateTime, SpuDO::getId);
        if (StrUtil.isNotBlank(keyword)) {
            wrapper.like(SpuDO::getName, StrUtil.trim(keyword));
        }
        if (categoryId != null) {
            wrapper.eq(SpuDO::getCategoryId, categoryId);
        }
        if (brandId != null) {
            wrapper.eq(SpuDO::getBrandId, brandId);
        }
        if (status != null) {
            wrapper.eq(SpuDO::getStatus, status);
        }
        return selectPage(page, wrapper);
    }

}
