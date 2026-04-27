package com.sparkleshop.service.stock.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sparkleshop.service.stock.entity.SkuStockDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface SkuStockMapper extends BaseMapper<SkuStockDO> {

    default SkuStockDO selectBySkuId(Long skuId) {
        return selectOne(new LambdaQueryWrapper<SkuStockDO>()
                .eq(SkuStockDO::getSkuId, skuId)
                .last("limit 1"));
    }

    @Update("""
            update sku_stock
            set locked_stock = locked_stock + #{quantity}
            where sku_id = #{skuId}
              and deleted = 0
              and stock - locked_stock >= #{quantity}
            """)
    int lockStock(@Param("skuId") Long skuId, @Param("quantity") Integer quantity);

    @Update("""
            update sku_stock
            set locked_stock = locked_stock - #{quantity}
            where sku_id = #{skuId}
              and deleted = 0
              and locked_stock >= #{quantity}
            """)
    int unlockStock(@Param("skuId") Long skuId, @Param("quantity") Integer quantity);

    @Update("""
            update sku_stock
            set stock = stock - #{quantity},
                locked_stock = locked_stock - #{quantity}
            where sku_id = #{skuId}
              and deleted = 0
              and locked_stock >= #{quantity}
              and stock >= #{quantity}
            """)
    int confirmStock(@Param("skuId") Long skuId, @Param("quantity") Integer quantity);
}
