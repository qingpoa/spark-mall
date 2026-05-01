package com.sparkleshop.service.coupon.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sparkleshop.service.coupon.entity.UserCouponDO;
import com.sparkleshop.service.coupon.enums.CouponStatusEnum;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Mapper
public interface UserCouponMapper extends BaseMapper<UserCouponDO> {

    default long countByUserIdAndTemplateId(Long userId, Long templateId) {
        return selectCount(new LambdaQueryWrapper<UserCouponDO>()
                .eq(UserCouponDO::getUserId, userId)
                .eq(UserCouponDO::getTemplateId, templateId));
    }

    default Map<Long, Long> countByUserIdAndTemplateIds(Long userId, Collection<Long> templateIds) {
        if (templateIds == null || templateIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<Map<String, Object>> rows = selectMaps(new QueryWrapper<UserCouponDO>()
                .select("template_id", "COUNT(*) AS received_count")
                .eq("user_id", userId)
                .in("template_id", templateIds)
                .groupBy("template_id"));
        return rows.stream().collect(Collectors.toMap(
                row -> ((Number) row.get("template_id")).longValue(),
                row -> ((Number) row.get("received_count")).longValue()
        ));
    }

    default UserCouponDO selectByIdAndUserId(Long couponId, Long userId) {
        return selectOne(new LambdaQueryWrapper<UserCouponDO>()
                .eq(UserCouponDO::getId, couponId)
                .eq(UserCouponDO::getUserId, userId));
    }

    default int occupyCoupon(Long couponId, Long userId, Long orderId, LocalDateTime now) {
        return update(null, new LambdaUpdateWrapper<UserCouponDO>()
                .eq(UserCouponDO::getId, couponId)
                .eq(UserCouponDO::getUserId, userId)
                .eq(UserCouponDO::getStatus, CouponStatusEnum.UNUSED.getCode())
                .isNull(UserCouponDO::getOrderId)
                .set(UserCouponDO::getStatus, CouponStatusEnum.OCCUPIED.getCode())
                .set(UserCouponDO::getOrderId, orderId)
                .set(UserCouponDO::getUpdateTime, now));
    }

    default int useCoupon(Long couponId, Long userId, Long orderId, LocalDateTime now) {
        return update(null, new LambdaUpdateWrapper<UserCouponDO>()
                .eq(UserCouponDO::getId, couponId)
                .eq(UserCouponDO::getUserId, userId)
                .eq(UserCouponDO::getStatus, CouponStatusEnum.OCCUPIED.getCode())
                .eq(UserCouponDO::getOrderId, orderId)
                .set(UserCouponDO::getStatus, CouponStatusEnum.USED.getCode())
                .set(UserCouponDO::getUseTime, now)
                .set(UserCouponDO::getUpdateTime, now));
    }

    default int rollbackCoupon(Long couponId, Long userId, Long orderId, Integer targetStatus, LocalDateTime now) {
        return update(null, new LambdaUpdateWrapper<UserCouponDO>()
                .eq(UserCouponDO::getId, couponId)
                .eq(UserCouponDO::getUserId, userId)
                .eq(UserCouponDO::getStatus, CouponStatusEnum.OCCUPIED.getCode())
                .eq(UserCouponDO::getOrderId, orderId)
                .set(UserCouponDO::getStatus, targetStatus)
                .set(UserCouponDO::getOrderId, null)
                .set(UserCouponDO::getUpdateTime, now));
    }

    default int expireCoupon(Long couponId, Long userId, LocalDateTime now) {
        return update(null, new LambdaUpdateWrapper<UserCouponDO>()
                .eq(UserCouponDO::getId, couponId)
                .eq(UserCouponDO::getUserId, userId)
                .eq(UserCouponDO::getStatus, CouponStatusEnum.UNUSED.getCode())
                .set(UserCouponDO::getStatus, CouponStatusEnum.EXPIRED.getCode())
                .set(UserCouponDO::getUpdateTime, now));
    }

    default int expireExpiredCouponsByUserId(Long userId, LocalDateTime now) {
        return update(null, new LambdaUpdateWrapper<UserCouponDO>()
                .eq(UserCouponDO::getUserId, userId)
                .eq(UserCouponDO::getStatus, CouponStatusEnum.UNUSED.getCode())
                .le(UserCouponDO::getExpireTime, now)
                .set(UserCouponDO::getStatus, CouponStatusEnum.EXPIRED.getCode())
                .set(UserCouponDO::getUpdateTime, now));
    }

    default int expireAllExpiredCoupons(LocalDateTime now) {
        return update(null, new LambdaUpdateWrapper<UserCouponDO>()
                .eq(UserCouponDO::getStatus, CouponStatusEnum.UNUSED.getCode())
                .le(UserCouponDO::getExpireTime, now)
                .set(UserCouponDO::getStatus, CouponStatusEnum.EXPIRED.getCode())
                .set(UserCouponDO::getUpdateTime, now));
    }

    default Page<UserCouponDO> selectUserCouponPage(Page<UserCouponDO> page, Long userId, Integer status) {
        LambdaQueryWrapper<UserCouponDO> wrapper = new LambdaQueryWrapper<UserCouponDO>()
                .eq(UserCouponDO::getUserId, userId);
        if (status != null) {
            wrapper.eq(UserCouponDO::getStatus, status);
        }
        wrapper.orderByAsc(UserCouponDO::getStatus)
                .orderByAsc(UserCouponDO::getExpireTime)
                .orderByDesc(UserCouponDO::getId);
        return selectPage(page, wrapper);
    }
}
