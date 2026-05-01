package com.sparkleshop.service.coupon.service.impl;

import cn.hutool.core.lang.UUID;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sparkleshop.common.core.exception.BusinessException;
import com.sparkleshop.common.core.model.Result;
import com.sparkleshop.common.security.jwt.LoginUserContext;
import com.sparkleshop.service.coupon.constant.CouponErrorCodes;
import com.sparkleshop.service.coupon.constant.CouponRedisKeys;
import com.sparkleshop.service.coupon.dto.CouponListQueryDTO;
import com.sparkleshop.service.coupon.dto.CouponReceiveRequest;
import com.sparkleshop.service.coupon.dto.CouponValidateQueryDTO;
import com.sparkleshop.service.coupon.dto.MyCouponQueryDTO;
import com.sparkleshop.service.coupon.dto.internal.CouponOccupyRequest;
import com.sparkleshop.service.coupon.dto.internal.CouponRollbackRequest;
import com.sparkleshop.service.coupon.dto.internal.CouponUseRequest;
import com.sparkleshop.service.coupon.dto.internal.CouponValidateRequest;
import com.sparkleshop.service.coupon.entity.CouponTemplateDO;
import com.sparkleshop.service.coupon.entity.UserCouponDO;
import com.sparkleshop.service.coupon.enums.CouponStatusEnum;
import com.sparkleshop.service.coupon.enums.CouponTypeEnum;
import com.sparkleshop.service.coupon.enums.CouponValidTypeEnum;
import com.sparkleshop.service.coupon.mapper.CouponTemplateMapper;
import com.sparkleshop.service.coupon.mapper.UserCouponMapper;
import com.sparkleshop.service.coupon.service.CouponService;
import com.sparkleshop.service.coupon.vo.CouponCheckRespVO;
import com.sparkleshop.service.coupon.vo.CouponListRespVO;
import com.sparkleshop.service.coupon.vo.CouponReceiveRespVO;
import com.sparkleshop.service.coupon.vo.CouponValidateRespVO;
import com.sparkleshop.service.coupon.vo.MyCouponPageRespVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
@RequiredArgsConstructor
public class CouponServiceImpl implements CouponService {

    private static final int TEMPLATE_STATUS_ENABLED = 1;
    private static final BigDecimal ZERO = BigDecimal.ZERO;
    private static final BigDecimal ONE = BigDecimal.ONE;
    private static final int COUPON_CODE_RETRY_TIMES = 3;
    private static final long RECEIVE_LOCK_WAIT_SECONDS = 3L;

    private final UserCouponMapper userCouponMapper;
    private final CouponTemplateMapper couponTemplateMapper;
    private final RedissonClient redissonClient;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CouponReceiveRespVO receiveCoupon(CouponReceiveRequest request) {
        Long userId = LoginUserContext.getRequiredUserId();
        Long templateId = request.getTemplateId();
        RLock lock = redissonClient.getLock(CouponRedisKeys.couponReceiveLock(userId, templateId));
        try {
            if (!lock.tryLock(RECEIVE_LOCK_WAIT_SECONDS, TimeUnit.SECONDS)) {
                throw new BusinessException(Result.CONFLICT, "领券过于频繁，请稍后再试");
            }
            return doReceiveCoupon(userId, templateId);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new BusinessException(Result.SERVER_ERROR, "领券操作被中断");
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    @Override
    public CouponListRespVO getReceivableCouponPage(CouponListQueryDTO queryDTO) {
        Long userId = LoginUserContext.getRequiredUserId();
        int pageNo = normalizePageNo(queryDTO.getPageNo());
        int pageSize = normalizePageSize(queryDTO.getPageSize());
        LocalDateTime now = LocalDateTime.now();
        Page<CouponTemplateDO> page = couponTemplateMapper.selectReceivableTemplatePage(new Page<>(pageNo, pageSize), now);

        CouponListRespVO response = new CouponListRespVO();
        response.setTotal(page.getTotal());
        response.setPageNo(pageNo);
        response.setPageSize(pageSize);
        List<CouponTemplateDO> templates = page.getRecords();
        if (templates == null || templates.isEmpty()) {
            response.setList(Collections.emptyList());
            return response;
        }

        Map<Long, Long> receivedCountMap = userCouponMapper.countByUserIdAndTemplateIds(userId, templates.stream()
                .map(CouponTemplateDO::getId)
                .toList());

        List<CouponListRespVO.Item> list = new ArrayList<>(templates.size());
        for (CouponTemplateDO template : templates) {
            CouponListRespVO.Item item = new CouponListRespVO.Item();
            item.setTemplateId(template.getId());
            item.setName(template.getName());
            item.setType(template.getType());
            item.setAmount(template.getAmount());
            item.setMinAmount(template.getMinAmount());
            item.setValidType(template.getValidType());
            item.setValidDays(template.getValidDays());
            item.setStartTime(template.getStartTime());
            item.setEndTime(template.getEndTime());
            item.setReceiveLimit(template.getReceiveLimit());
            item.setReceivedCount(receivedCountMap.getOrDefault(template.getId(), 0L));
            list.add(item);
        }
        response.setList(list);
        return response;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MyCouponPageRespVO getCurrentUserCouponPage(MyCouponQueryDTO queryDTO) {
        Long userId = LoginUserContext.getRequiredUserId();
        LocalDateTime now = LocalDateTime.now();
        userCouponMapper.expireExpiredCouponsByUserId(userId, now);

        int pageNo = normalizePageNo(queryDTO.getPageNo());
        int pageSize = normalizePageSize(queryDTO.getPageSize());
        Page<UserCouponDO> page = userCouponMapper.selectUserCouponPage(new Page<>(pageNo, pageSize), userId, queryDTO.getStatus());

        MyCouponPageRespVO response = new MyCouponPageRespVO();
        response.setTotal(page.getTotal());
        response.setPageNo(page.getCurrent());
        response.setPageSize(page.getSize());

        List<UserCouponDO> coupons = page.getRecords();
        if (coupons == null || coupons.isEmpty()) {
            response.setList(Collections.emptyList());
            return response;
        }

        Map<Long, CouponTemplateDO> templateMap = couponTemplateMapper.selectBatchIds(extractTemplateIds(coupons)).stream()
                .collect(Collectors.toMap(CouponTemplateDO::getId, Function.identity(), (left, right) -> left, LinkedHashMap::new));

        List<MyCouponPageRespVO.Item> list = new ArrayList<>(coupons.size());
        for (UserCouponDO coupon : coupons) {
            CouponTemplateDO template = templateMap.get(coupon.getTemplateId());
            MyCouponPageRespVO.Item item = new MyCouponPageRespVO.Item();
            item.setCouponId(coupon.getId());
            item.setTemplateId(coupon.getTemplateId());
            item.setName(template == null ? null : template.getName());
            item.setType(template == null ? null : template.getType());
            item.setAmount(template == null ? null : template.getAmount());
            item.setMinAmount(template == null ? null : template.getMinAmount());
            item.setStatus(coupon.getStatus());
            item.setReceiveTime(coupon.getReceiveTime());
            item.setExpireTime(coupon.getExpireTime());
            item.setUseTime(coupon.getUseTime());
            list.add(item);
        }
        response.setList(list);
        return response;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CouponCheckRespVO validateCurrentUserCoupon(Long couponId, CouponValidateQueryDTO queryDTO) {
        Long userId = LoginUserContext.getRequiredUserId();
        UserCouponDO userCoupon = userCouponMapper.selectByIdAndUserId(couponId, userId);
        userCoupon = refreshUnusedCouponIfExpired(userCoupon);
        CouponTemplateDO template = userCoupon == null ? null : couponTemplateMapper.selectById(userCoupon.getTemplateId());
        CouponEvaluation evaluation = evaluateCoupon(userCoupon, template, queryDTO.getOrderAmount(), LocalDateTime.now());

        CouponCheckRespVO response = new CouponCheckRespVO();
        response.setValid(evaluation.available());
        response.setReason(evaluation.reason());
        response.setDiscountAmount(evaluation.discountAmount());
        return response;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CouponValidateRespVO validateCoupon(CouponValidateRequest request) {
        UserCouponDO userCoupon = userCouponMapper.selectByIdAndUserId(request.getCouponId(), request.getUserId());
        userCoupon = refreshUnusedCouponIfExpired(userCoupon);
        CouponTemplateDO template = userCoupon == null ? null : couponTemplateMapper.selectById(userCoupon.getTemplateId());
        CouponEvaluation evaluation = evaluateCoupon(userCoupon, template, request.getOrderAmount(), LocalDateTime.now());
        CouponValidateRespVO response = buildUnavailableResponse(request.getCouponId());
        response.setAvailable(evaluation.available());
        response.setDiscountAmount(evaluation.discountAmount());
        return response;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void occupyCoupon(CouponOccupyRequest request) {
        UserCouponDO userCoupon = getRequiredUserCoupon(request.getCouponId(), request.getUserId());
        userCoupon = refreshUnusedCouponIfExpired(userCoupon);

        if (userCoupon == null) {
            throw new BusinessException(CouponErrorCodes.COUPON_NOT_FOUND, "用户优惠券不存在");
        }
        if (CouponStatusEnum.isOccupied(userCoupon.getStatus()) && Objects.equals(userCoupon.getOrderId(), request.getOrderId())) {
            return;
        }
        if (!CouponStatusEnum.isUnused(userCoupon.getStatus())) {
            throw new BusinessException(CouponErrorCodes.COUPON_UNAVAILABLE, "优惠券当前不可占用");
        }

        CouponTemplateDO template = getRequiredTemplate(userCoupon.getTemplateId());
        if (!isTemplateEnabled(template)) {
            throw new BusinessException(CouponErrorCodes.COUPON_UNAVAILABLE, "优惠券模板已禁用");
        }
        if (isExpired(userCoupon.getExpireTime(), LocalDateTime.now())) {
            refreshUnusedCouponIfExpired(userCoupon);
            throw new BusinessException(CouponErrorCodes.COUPON_UNAVAILABLE, "优惠券已过期");
        }
        if (!isTemplateAvailableNow(template, LocalDateTime.now())) {
            throw new BusinessException(CouponErrorCodes.COUPON_UNAVAILABLE, "优惠券当前不在可用时间范围内");
        }

        int rows = userCouponMapper.occupyCoupon(
                request.getCouponId(),
                request.getUserId(),
                request.getOrderId(),
                LocalDateTime.now()
        );
        if (rows != 1) {
            throw new BusinessException(CouponErrorCodes.COUPON_UNAVAILABLE, "优惠券占用失败");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void useCoupon(CouponUseRequest request) {
        UserCouponDO userCoupon = getRequiredUserCoupon(request.getCouponId(), request.getUserId());
        LocalDateTime now = LocalDateTime.now();
        if (CouponStatusEnum.isUsed(userCoupon.getStatus()) && Objects.equals(userCoupon.getOrderId(), request.getOrderId())) {
            return;
        }
        if (!CouponStatusEnum.isOccupied(userCoupon.getStatus()) || !Objects.equals(userCoupon.getOrderId(), request.getOrderId())) {
            throw new BusinessException(CouponErrorCodes.COUPON_STATUS_INVALID, "优惠券未被当前订单占用");
        }
        if (isExpired(userCoupon.getExpireTime(), now)) {
            throw new BusinessException(CouponErrorCodes.COUPON_UNAVAILABLE, "优惠券已过期，无法核销");
        }

        int rows = userCouponMapper.useCoupon(
                request.getCouponId(),
                request.getUserId(),
                request.getOrderId(),
                now
        );
        if (rows != 1) {
            throw new BusinessException(CouponErrorCodes.COUPON_STATUS_INVALID, "优惠券核销失败");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void rollbackCoupon(CouponRollbackRequest request) {
        UserCouponDO userCoupon = getRequiredUserCoupon(request.getCouponId(), request.getUserId());
        if ((CouponStatusEnum.isUnused(userCoupon.getStatus()) || CouponStatusEnum.isExpired(userCoupon.getStatus()))
                && userCoupon.getOrderId() == null) {
            return;
        }
        if (!CouponStatusEnum.isOccupied(userCoupon.getStatus()) || !Objects.equals(userCoupon.getOrderId(), request.getOrderId())) {
            throw new BusinessException(CouponErrorCodes.COUPON_STATUS_INVALID, "优惠券未被当前订单占用");
        }

        LocalDateTime now = LocalDateTime.now();
        Integer targetStatus = isExpired(userCoupon.getExpireTime(), now)
                ? CouponStatusEnum.EXPIRED.getCode()
                : CouponStatusEnum.UNUSED.getCode();

        int rows = userCouponMapper.rollbackCoupon(
                request.getCouponId(),
                request.getUserId(),
                request.getOrderId(),
                targetStatus,
                now
        );
        if (rows != 1) {
            throw new BusinessException(CouponErrorCodes.COUPON_STATUS_INVALID, "优惠券回退失败");
        }
    }

    @Scheduled(fixedDelay = 60000)
    public void scheduleExpireCoupons() {
        try {
            int rows = expireOverdueCoupons(LocalDateTime.now());
            if (rows > 0) {
                log.info("expired overdue coupons, rows={}", rows);
            }
        } catch (Exception exception) {
            log.error("expire overdue coupons failed", exception);
        }
    }

    private CouponValidateRespVO buildUnavailableResponse(Long couponId) {
        CouponValidateRespVO response = new CouponValidateRespVO();
        response.setCouponId(couponId);
        response.setDiscountAmount(ZERO);
        response.setAvailable(Boolean.FALSE);
        return response;
    }

    private CouponReceiveRespVO doReceiveCoupon(Long userId, Long templateId) {
        CouponTemplateDO template = getRequiredTemplate(templateId);
        LocalDateTime now = LocalDateTime.now();

        validateReceivableTemplate(template, now);
        validateReceiveLimit(userId, template);

        int rows = couponTemplateMapper.increaseIssuedCountIfAvailable(template.getId(), now);
        if (rows != 1) {
            throw new BusinessException(CouponErrorCodes.COUPON_ISSUED_OUT, "优惠券已领完");
        }

        UserCouponDO userCoupon = createUserCoupon(userId, template, now);
        CouponReceiveRespVO response = new CouponReceiveRespVO();
        response.setCouponId(userCoupon.getId());
        response.setCouponCode(userCoupon.getCouponCode());
        response.setExpireTime(userCoupon.getExpireTime());
        return response;
    }

    private CouponEvaluation evaluateCoupon(UserCouponDO userCoupon,
                                            CouponTemplateDO template,
                                            BigDecimal orderAmount,
                                            LocalDateTime now) {
        if (userCoupon == null) {
            return CouponEvaluation.invalid("优惠券不存在");
        }
        if (!CouponStatusEnum.isUnused(userCoupon.getStatus())) {
            return CouponEvaluation.invalid(resolveCouponUnavailableReason(userCoupon.getStatus()));
        }
        if (template == null) {
            return CouponEvaluation.invalid("优惠券模板不存在");
        }
        if (!isTemplateEnabled(template)) {
            return CouponEvaluation.invalid("优惠券模板已禁用");
        }
        if (!isTemplateAvailableNow(template, now)) {
            return CouponEvaluation.invalid("优惠券当前不在可用时间范围内");
        }

        CouponDiscountEvaluation discountEvaluation = calculateDiscountAmountWithReason(template, orderAmount);
        if (!discountEvaluation.available()) {
            return CouponEvaluation.invalid(discountEvaluation.reason());
        }
        return CouponEvaluation.valid(discountEvaluation.discountAmount());
    }

    private UserCouponDO getRequiredUserCoupon(Long couponId, Long userId) {
        UserCouponDO userCoupon = userCouponMapper.selectByIdAndUserId(couponId, userId);
        if (userCoupon == null) {
            throw new BusinessException(CouponErrorCodes.COUPON_NOT_FOUND, "用户优惠券不存在");
        }
        return userCoupon;
    }

    private CouponTemplateDO getRequiredTemplate(Long templateId) {
        CouponTemplateDO template = couponTemplateMapper.selectById(templateId);
        if (template == null) {
            throw new BusinessException(CouponErrorCodes.COUPON_TEMPLATE_NOT_FOUND, "优惠券模板不存在");
        }
        return template;
    }

    private void validateReceivableTemplate(CouponTemplateDO template, LocalDateTime now) {
        if (!isTemplateEnabled(template)) {
            throw new BusinessException(CouponErrorCodes.COUPON_UNAVAILABLE, "优惠券模板已禁用");
        }
        if (!isTemplateAvailableNow(template, now)) {
            throw new BusinessException(CouponErrorCodes.COUPON_UNAVAILABLE, "优惠券当前不在可领取时间范围内");
        }
    }

    private void validateReceiveLimit(Long userId, CouponTemplateDO template) {
        Integer receiveLimit = template.getReceiveLimit();
        if (receiveLimit == null || receiveLimit <= 0) {
            return;
        }

        long receivedCount = userCouponMapper.countByUserIdAndTemplateId(userId, template.getId());
        if (receivedCount >= receiveLimit) {
            throw new BusinessException(CouponErrorCodes.COUPON_RECEIVE_LIMIT_EXCEEDED, "已达到该优惠券领取上限");
        }
    }

    private String resolveCouponUnavailableReason(Integer status) {
        if (CouponStatusEnum.USED.getCode().equals(status)) {
            return "优惠券已使用";
        }
        if (CouponStatusEnum.EXPIRED.getCode().equals(status)) {
            return "优惠券已过期";
        }
        if (CouponStatusEnum.INVALID.getCode().equals(status)) {
            return "优惠券已失效";
        }
        if (CouponStatusEnum.OCCUPIED.getCode().equals(status)) {
            return "优惠券已被占用";
        }
        return "优惠券当前不可用";
    }

    private UserCouponDO refreshUnusedCouponIfExpired(UserCouponDO userCoupon) {
        if (userCoupon == null || !CouponStatusEnum.isUnused(userCoupon.getStatus())) {
            return userCoupon;
        }

        LocalDateTime now = LocalDateTime.now();
        if (!isExpired(userCoupon.getExpireTime(), now)) {
            return userCoupon;
        }

        int rows = userCouponMapper.expireCoupon(userCoupon.getId(), userCoupon.getUserId(), now);
        if (rows == 1) {
            userCoupon.setStatus(CouponStatusEnum.EXPIRED.getCode());
            userCoupon.setUpdateTime(now);
            return userCoupon;
        }
        return userCouponMapper.selectByIdAndUserId(userCoupon.getId(), userCoupon.getUserId());
    }

    private int expireOverdueCoupons(LocalDateTime now) {
        return userCouponMapper.expireAllExpiredCoupons(now);
    }

    private UserCouponDO createUserCoupon(Long userId, CouponTemplateDO template, LocalDateTime now) {
        LocalDateTime expireTime = resolveExpireTime(template, now);
        for (int i = 0; i < COUPON_CODE_RETRY_TIMES; i++) {
            UserCouponDO userCoupon = new UserCouponDO();
            userCoupon.setUserId(userId);
            userCoupon.setTemplateId(template.getId());
            userCoupon.setCouponCode(generateCouponCode());
            userCoupon.setStatus(CouponStatusEnum.UNUSED.getCode());
            userCoupon.setReceiveTime(now);
            userCoupon.setExpireTime(expireTime);
            userCoupon.setOrderId(null);
            userCoupon.setUseTime(null);
            userCoupon.setCreateTime(now);
            userCoupon.setUpdateTime(now);
            userCoupon.setDeleted(0);
            try {
                userCouponMapper.insert(userCoupon);
                return userCoupon;
            } catch (DuplicateKeyException exception) {
                if (i == COUPON_CODE_RETRY_TIMES - 1) {
                    throw new BusinessException(CouponErrorCodes.COUPON_UNAVAILABLE, "生成优惠券码失败");
                }
            }
        }
        throw new BusinessException(CouponErrorCodes.COUPON_UNAVAILABLE, "生成优惠券码失败");
    }

    private LocalDateTime resolveExpireTime(CouponTemplateDO template, LocalDateTime now) {
        if (CouponValidTypeEnum.FIXED_DAYS.getCode().equals(template.getValidType())) {
            Integer validDays = template.getValidDays();
            if (validDays == null || validDays <= 0) {
                throw new BusinessException(CouponErrorCodes.INVALID_REQUEST, "优惠券有效天数配置错误");
            }
            return now.plusDays(validDays);
        }
        if (CouponValidTypeEnum.FIXED_RANGE.getCode().equals(template.getValidType())) {
            if (template.getEndTime() == null) {
                throw new BusinessException(CouponErrorCodes.INVALID_REQUEST, "优惠券结束时间配置错误");
            }
            return template.getEndTime();
        }
        throw new BusinessException(CouponErrorCodes.INVALID_REQUEST, "优惠券有效期类型不支持");
    }

    private String generateCouponCode() {
        return UUID.fastUUID().toString(true).toUpperCase();
    }

    private List<Long> extractTemplateIds(Collection<UserCouponDO> coupons) {
        return coupons.stream()
                .map(UserCouponDO::getTemplateId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }

    private boolean isTemplateEnabled(CouponTemplateDO template) {
        return template != null && Integer.valueOf(TEMPLATE_STATUS_ENABLED).equals(template.getStatus());
    }

    private boolean isTemplateAvailableNow(CouponTemplateDO template, LocalDateTime now) {
        if (template == null) {
            return false;
        }

        if (CouponValidTypeEnum.FIXED_RANGE.getCode().equals(template.getValidType())) {
            if (template.getStartTime() != null && now.isBefore(template.getStartTime())) {
                return false;
            }
            if (template.getEndTime() != null && !template.getEndTime().isAfter(now)) {
                return false;
            }
        }
        return true;
    }

    private boolean isExpired(LocalDateTime expireTime, LocalDateTime now) {
        return expireTime != null && !expireTime.isAfter(now);
    }

    private CouponDiscountEvaluation calculateDiscountAmountWithReason(CouponTemplateDO template, BigDecimal orderAmount) {
        if (template == null || orderAmount == null || orderAmount.compareTo(ZERO) <= 0) {
            return CouponDiscountEvaluation.invalid("订单金额异常");
        }

        CouponTypeEnum couponType = CouponTypeEnum.fromCode(template.getType());
        if (couponType == null) {
            return CouponDiscountEvaluation.invalid("优惠券类型不支持");
        }

        BigDecimal amount = template.getAmount();
        if (amount == null || amount.compareTo(ZERO) <= 0) {
            return CouponDiscountEvaluation.invalid("优惠券金额配置错误");
        }

        BigDecimal minAmount = template.getMinAmount() == null ? ZERO : template.getMinAmount();
        if (couponType != CouponTypeEnum.NO_THRESHOLD && orderAmount.compareTo(minAmount) < 0) {
            return CouponDiscountEvaluation.invalid("未达到优惠券使用门槛");
        }

        BigDecimal discountAmount = switch (couponType) {
            case FULL_REDUCTION, NO_THRESHOLD -> clampDiscount(amount, orderAmount);
            case DISCOUNT -> calculateDiscountCouponAmount(orderAmount, amount);
        };
        if (discountAmount == null || discountAmount.compareTo(ZERO) <= 0) {
            return CouponDiscountEvaluation.invalid(couponType == CouponTypeEnum.DISCOUNT ? "优惠券折扣配置错误" : "优惠券不可用");
        }
        return CouponDiscountEvaluation.valid(discountAmount);
    }

    private BigDecimal calculateDiscountCouponAmount(BigDecimal orderAmount, BigDecimal discountRate) {
        if (discountRate.compareTo(ZERO) <= 0 || discountRate.compareTo(ONE) >= 0) {
            return null;
        }
        BigDecimal discountAmount = orderAmount.multiply(ONE.subtract(discountRate))
                .setScale(2, RoundingMode.HALF_UP);
        return clampDiscount(discountAmount, orderAmount);
    }

    private BigDecimal clampDiscount(BigDecimal discountAmount, BigDecimal orderAmount) {
        if (discountAmount == null || discountAmount.compareTo(ZERO) <= 0) {
            return null;
        }
        return discountAmount.min(orderAmount).setScale(2, RoundingMode.HALF_UP);
    }

    private int normalizePageNo(Integer pageNo) {
        return pageNo == null || pageNo <= 0 ? 1 : pageNo;
    }

    private int normalizePageSize(Integer pageSize) {
        return pageSize == null || pageSize <= 0 ? 10 : pageSize;
    }

    private record CouponEvaluation(boolean available, BigDecimal discountAmount, String reason) {
        private static CouponEvaluation valid(BigDecimal discountAmount) {
            return new CouponEvaluation(true, discountAmount == null ? ZERO : discountAmount, null);
        }

        private static CouponEvaluation invalid(String reason) {
            return new CouponEvaluation(false, ZERO, reason);
        }
    }

    private record CouponDiscountEvaluation(boolean available, BigDecimal discountAmount, String reason) {
        private static CouponDiscountEvaluation valid(BigDecimal discountAmount) {
            return new CouponDiscountEvaluation(true, discountAmount, null);
        }

        private static CouponDiscountEvaluation invalid(String reason) {
            return new CouponDiscountEvaluation(false, ZERO, reason);
        }
    }
}
