package com.sparkleshop.service.product.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sparkleshop.service.product.entity.CategoryDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface CategoryMapper extends BaseMapper<CategoryDO> {

    default List<CategoryDO> selectEnabledList() {
        return selectList(new LambdaQueryWrapper<CategoryDO>()
                .eq(CategoryDO::getStatus, 1)
                .orderByAsc(CategoryDO::getSort, CategoryDO::getId));
    }

    default List<CategoryDO> selectAdminList() {
        return selectList(new LambdaQueryWrapper<CategoryDO>()
                .orderByAsc(CategoryDO::getSort, CategoryDO::getId));
    }
}
