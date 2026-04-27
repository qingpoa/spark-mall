package com.sparkleshop.service.coupon.service;

import com.sparkleshop.service.coupon.dto.internal.CouponOccupyRequest;
import com.sparkleshop.service.coupon.dto.internal.CouponRollbackRequest;
import com.sparkleshop.service.coupon.dto.internal.CouponUseRequest;
import com.sparkleshop.service.coupon.dto.internal.CouponValidateRequest;
import com.sparkleshop.service.coupon.vo.CouponValidateRespVO;

public interface CouponService {

    CouponValidateRespVO validateCoupon(CouponValidateRequest request);

    void occupyCoupon(CouponOccupyRequest request);

    void useCoupon(CouponUseRequest request);

    void rollbackCoupon(CouponRollbackRequest request);
}
