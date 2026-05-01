package com.sparkleshop.service.coupon.service;

import com.sparkleshop.service.coupon.dto.CouponListQueryDTO;
import com.sparkleshop.service.coupon.dto.CouponReceiveRequest;
import com.sparkleshop.service.coupon.dto.CouponValidateQueryDTO;
import com.sparkleshop.service.coupon.dto.MyCouponQueryDTO;
import com.sparkleshop.service.coupon.dto.internal.CouponOccupyRequest;
import com.sparkleshop.service.coupon.dto.internal.CouponRollbackRequest;
import com.sparkleshop.service.coupon.dto.internal.CouponUseRequest;
import com.sparkleshop.service.coupon.dto.internal.CouponValidateRequest;
import com.sparkleshop.service.coupon.vo.CouponCheckRespVO;
import com.sparkleshop.service.coupon.vo.CouponListRespVO;
import com.sparkleshop.service.coupon.vo.CouponReceiveRespVO;
import com.sparkleshop.service.coupon.vo.CouponValidateRespVO;
import com.sparkleshop.service.coupon.vo.MyCouponPageRespVO;

public interface CouponService {

    CouponReceiveRespVO receiveCoupon(CouponReceiveRequest request);

    CouponListRespVO getReceivableCouponPage(CouponListQueryDTO queryDTO);

    MyCouponPageRespVO getCurrentUserCouponPage(MyCouponQueryDTO queryDTO);

    CouponCheckRespVO validateCurrentUserCoupon(Long couponId, CouponValidateQueryDTO queryDTO);

    CouponValidateRespVO validateCoupon(CouponValidateRequest request);

    void occupyCoupon(CouponOccupyRequest request);

    void useCoupon(CouponUseRequest request);

    void rollbackCoupon(CouponRollbackRequest request);
}
