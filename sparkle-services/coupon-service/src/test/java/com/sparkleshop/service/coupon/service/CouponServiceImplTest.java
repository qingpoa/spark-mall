package com.sparkleshop.service.coupon.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sparkleshop.common.core.exception.BusinessException;
import com.sparkleshop.common.security.jwt.LoginUserContext;
import com.sparkleshop.common.security.jwt.TokenUser;
import com.sparkleshop.service.coupon.dto.CouponListQueryDTO;
import com.sparkleshop.service.coupon.dto.CouponReceiveRequest;
import com.sparkleshop.service.coupon.dto.CouponValidateQueryDTO;
import com.sparkleshop.service.coupon.dto.internal.CouponUseRequest;
import com.sparkleshop.service.coupon.entity.CouponTemplateDO;
import com.sparkleshop.service.coupon.entity.UserCouponDO;
import com.sparkleshop.service.coupon.enums.CouponStatusEnum;
import com.sparkleshop.service.coupon.enums.CouponValidTypeEnum;
import com.sparkleshop.service.coupon.mapper.CouponTemplateMapper;
import com.sparkleshop.service.coupon.mapper.UserCouponMapper;
import com.sparkleshop.service.coupon.service.impl.CouponServiceImpl;
import com.sparkleshop.service.coupon.vo.CouponCheckRespVO;
import com.sparkleshop.service.coupon.vo.CouponReceiveRespVO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CouponServiceImplTest {

    @Mock
    private UserCouponMapper userCouponMapper;
    @Mock
    private CouponTemplateMapper couponTemplateMapper;
    @Mock
    private RedissonClient redissonClient;
    @Mock
    private RLock receiveLock;

    @InjectMocks
    private CouponServiceImpl couponService;

    @BeforeEach
    void setUp() {
        TokenUser tokenUser = new TokenUser();
        tokenUser.setUserId(1001L);
        tokenUser.setUserType(1);
        tokenUser.setTokenId("token-1");
        LoginUserContext.set(tokenUser);
    }

    @AfterEach
    void tearDown() {
        LoginUserContext.clear();
    }

    @Test
    void shouldReceiveCouponSuccessfully() throws InterruptedException {
        CouponReceiveRequest request = new CouponReceiveRequest();
        request.setTemplateId(10L);

        CouponTemplateDO template = buildTemplate(10L);

        when(redissonClient.getLock(anyString())).thenReturn(receiveLock);
        when(receiveLock.tryLock(anyLong(), any())).thenReturn(true);
        when(receiveLock.isHeldByCurrentThread()).thenReturn(true);
        when(couponTemplateMapper.selectById(10L)).thenReturn(template);
        when(userCouponMapper.countByUserIdAndTemplateId(1001L, 10L)).thenReturn(0L);
        when(couponTemplateMapper.increaseIssuedCountIfAvailable(anyLong(), any())).thenReturn(1);
        doAnswer(invocation -> {
            UserCouponDO userCoupon = invocation.getArgument(0);
            userCoupon.setId(2001L);
            return 1;
        }).when(userCouponMapper).insert(any(UserCouponDO.class));

        CouponReceiveRespVO response = couponService.receiveCoupon(request);

        assertEquals(2001L, response.getCouponId());
        assertNotNull(response.getCouponCode());
        assertNotNull(response.getExpireTime());
        verify(receiveLock).unlock();
    }

    @Test
    void shouldRejectReceiveWhenUserExceedsLimit() throws InterruptedException {
        CouponReceiveRequest request = new CouponReceiveRequest();
        request.setTemplateId(10L);

        CouponTemplateDO template = buildTemplate(10L);
        template.setReceiveLimit(1);

        when(redissonClient.getLock(anyString())).thenReturn(receiveLock);
        when(receiveLock.tryLock(anyLong(), any())).thenReturn(true);
        when(receiveLock.isHeldByCurrentThread()).thenReturn(true);
        when(couponTemplateMapper.selectById(10L)).thenReturn(template);
        when(userCouponMapper.countByUserIdAndTemplateId(1001L, 10L)).thenReturn(1L);

        BusinessException exception = assertThrows(BusinessException.class, () -> couponService.receiveCoupon(request));

        assertEquals(40907, exception.getCode());
        verify(receiveLock).unlock();
    }

    @Test
    void shouldValidateCurrentUserCouponWithReason() {
        CouponValidateQueryDTO queryDTO = new CouponValidateQueryDTO();
        queryDTO.setOrderAmount(new BigDecimal("50.00"));

        UserCouponDO userCoupon = buildUserCoupon(2001L, 1001L, 10L, CouponStatusEnum.UNUSED.getCode(), LocalDateTime.now().plusDays(1));
        CouponTemplateDO template = buildTemplate(10L);
        template.setMinAmount(new BigDecimal("100.00"));

        when(userCouponMapper.selectByIdAndUserId(2001L, 1001L)).thenReturn(userCoupon);
        when(couponTemplateMapper.selectById(10L)).thenReturn(template);

        CouponCheckRespVO response = couponService.validateCurrentUserCoupon(2001L, queryDTO);

        assertEquals(Boolean.FALSE, response.getValid());
        assertEquals("未达到优惠券使用门槛", response.getReason());
        assertEquals(0, response.getDiscountAmount().compareTo(BigDecimal.ZERO));
    }

    @Test
    void shouldListReceivableCouponsWithPagedTemplatesAndBatchReceivedCount() {
        CouponTemplateDO template = buildTemplate(10L);
        Page<CouponTemplateDO> page = new Page<>(1, 10);
        page.setTotal(1);
        page.setRecords(List.of(template));

        when(couponTemplateMapper.selectReceivableTemplatePage(any(), any())).thenReturn(page);
        when(userCouponMapper.countByUserIdAndTemplateIds(1001L, List.of(10L))).thenReturn(Map.of(10L, 2L));

        CouponListQueryDTO queryDTO = new CouponListQueryDTO();
        var response = couponService.getReceivableCouponPage(queryDTO);

        assertEquals(1L, response.getTotal());
        assertEquals(1, response.getList().size());
        assertEquals(2L, response.getList().get(0).getReceivedCount());
    }

    @Test
    void shouldRejectUseCouponWhenExpiredAfterOccupy() {
        LocalDateTime expiredAt = LocalDateTime.now().minusMinutes(1);
        UserCouponDO userCoupon = buildUserCoupon(2001L, 1001L, 10L, CouponStatusEnum.OCCUPIED.getCode(), expiredAt);
        userCoupon.setOrderId(3001L);

        CouponUseRequest request = new CouponUseRequest();
        request.setUserId(1001L);
        request.setCouponId(2001L);
        request.setOrderId(3001L);

        when(userCouponMapper.selectByIdAndUserId(2001L, 1001L)).thenReturn(userCoupon);

        BusinessException exception = assertThrows(BusinessException.class, () -> couponService.useCoupon(request));

        assertEquals(40905, exception.getCode());
    }

    @Test
    void shouldExpireOverdueCouponsWhenListingMyCoupons() {
        MyPageFixture fixture = buildMyCouponPageFixture();
        when(userCouponMapper.expireExpiredCouponsByUserId(anyLong(), any())).thenReturn(1);
        when(userCouponMapper.selectUserCouponPage(any(), anyLong(), any())).thenReturn(fixture.page());
        when(couponTemplateMapper.selectBatchIds(any())).thenReturn(fixture.templates());

        var response = couponService.getCurrentUserCouponPage(new com.sparkleshop.service.coupon.dto.MyCouponQueryDTO());

        assertEquals(1, response.getList().size());
        verify(userCouponMapper).expireExpiredCouponsByUserId(anyLong(), any());
    }

    private CouponTemplateDO buildTemplate(Long templateId) {
        CouponTemplateDO template = new CouponTemplateDO();
        template.setId(templateId);
        template.setName("满100减10");
        template.setType(1);
        template.setAmount(new BigDecimal("10.00"));
        template.setMinAmount(new BigDecimal("100.00"));
        template.setTotalCount(100);
        template.setIssuedCount(0);
        template.setReceiveLimit(2);
        template.setValidType(CouponValidTypeEnum.FIXED_DAYS.getCode());
        template.setValidDays(7);
        template.setStatus(1);
        template.setCreateTime(LocalDateTime.now());
        return template;
    }

    private UserCouponDO buildUserCoupon(Long couponId, Long userId, Long templateId, Integer status, LocalDateTime expireTime) {
        UserCouponDO userCoupon = new UserCouponDO();
        userCoupon.setId(couponId);
        userCoupon.setUserId(userId);
        userCoupon.setTemplateId(templateId);
        userCoupon.setCouponCode("CODE001");
        userCoupon.setStatus(status);
        userCoupon.setReceiveTime(LocalDateTime.now().minusDays(1));
        userCoupon.setExpireTime(expireTime);
        return userCoupon;
    }

    private MyPageFixture buildMyCouponPageFixture() {
        UserCouponDO coupon = buildUserCoupon(2001L, 1001L, 10L, CouponStatusEnum.UNUSED.getCode(), LocalDateTime.now().plusDays(1));
        Page<UserCouponDO> page = new Page<>(1, 10);
        page.setTotal(1);
        page.setRecords(List.of(coupon));

        CouponTemplateDO template = buildTemplate(10L);
        return new MyPageFixture(page, List.of(template));
    }

    private record MyPageFixture(Page<UserCouponDO> page, List<CouponTemplateDO> templates) {
    }
}
