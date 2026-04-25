package com.sparkleshop.service.coupon.service.impl;

import com.sparkleshop.service.coupon.dto.internal.CouponOccupyRequest;
import com.sparkleshop.service.coupon.dto.internal.CouponValidateRequest;
import com.sparkleshop.service.coupon.service.CouponService;
import com.sparkleshop.service.coupon.vo.CouponValidateRespVO;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class CouponServiceImpl implements CouponService {

    @Override
    public CouponValidateRespVO validateCoupon(CouponValidateRequest request) {
        CouponValidateRespVO response = new CouponValidateRespVO();
        response.setCouponId(request.getCouponId());
        response.setDiscountAmount(BigDecimal.ZERO);
        response.setAvailable(Boolean.TRUE);
        return response;
    }

    @Override
    public void occupyCoupon(CouponOccupyRequest request) {
        // 优惠券服务一期先补齐订单侧调用契约，后续在这里接入真实占用逻辑。
    }
}
