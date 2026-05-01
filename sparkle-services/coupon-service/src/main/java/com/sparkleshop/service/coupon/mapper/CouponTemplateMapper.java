package com.sparkleshop.service.coupon.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sparkleshop.service.coupon.entity.CouponTemplateDO;
import com.sparkleshop.service.coupon.enums.CouponValidTypeEnum;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDateTime;

@Mapper
public interface CouponTemplateMapper extends BaseMapper<CouponTemplateDO> {

    default int increaseIssuedCountIfAvailable(Long templateId, LocalDateTime now) {
        return update(null, new LambdaUpdateWrapper<CouponTemplateDO>()
                .eq(CouponTemplateDO::getId, templateId)
                .eq(CouponTemplateDO::getStatus, 1)
                .and(wrapper -> wrapper.eq(CouponTemplateDO::getTotalCount, 0)
                        .or()
                        .apply("issued_count < total_count"))
                .setSql("issued_count = issued_count + 1")
                .set(CouponTemplateDO::getUpdateTime, now));
    }

    default Page<CouponTemplateDO> selectReceivableTemplatePage(Page<CouponTemplateDO> page, LocalDateTime now) {
        LambdaQueryWrapper<CouponTemplateDO> wrapper = new LambdaQueryWrapper<CouponTemplateDO>()
                .eq(CouponTemplateDO::getStatus, 1)
                .and(query -> query.eq(CouponTemplateDO::getTotalCount, 0)
                        .or()
                        .apply("issued_count < total_count"))
                .and(query -> query.eq(CouponTemplateDO::getValidType, CouponValidTypeEnum.FIXED_DAYS.getCode())
                        .or(range -> range.eq(CouponTemplateDO::getValidType, CouponValidTypeEnum.FIXED_RANGE.getCode())
                                .and(timeWindow -> timeWindow.isNull(CouponTemplateDO::getStartTime)
                                        .or()
                                        .le(CouponTemplateDO::getStartTime, now))
                                .and(timeWindow -> timeWindow.isNull(CouponTemplateDO::getEndTime)
                                        .or()
                                        .gt(CouponTemplateDO::getEndTime, now))))
                .orderByDesc(CouponTemplateDO::getCreateTime)
                .orderByDesc(CouponTemplateDO::getId);
        return selectPage(page, wrapper);
    }
}
