package com.sparkleshop.service.coupon.controller.internal;

import com.sparkleshop.common.core.model.Result;
import com.sparkleshop.common.web.util.Results;
import com.sparkleshop.service.coupon.dto.internal.CouponOccupyRequest;
import com.sparkleshop.service.coupon.dto.internal.CouponRollbackRequest;
import com.sparkleshop.service.coupon.dto.internal.CouponUseRequest;
import com.sparkleshop.service.coupon.dto.internal.CouponValidateRequest;
import com.sparkleshop.service.coupon.service.CouponService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping({"", "/coupon"})
public class CouponInternalController {

    private final CouponService couponService;

    @PostMapping("/internal/validate")
    public ResponseEntity<Result> validateCoupon(@Valid @RequestBody CouponValidateRequest request) {
        return Results.ok(couponService.validateCoupon(request));
    }

    @PostMapping("/internal/occupy")
    public ResponseEntity<Result> occupyCoupon(@Valid @RequestBody CouponOccupyRequest request) {
        couponService.occupyCoupon(request);
        return Results.ok();
    }

    @PostMapping("/internal/use")
    public ResponseEntity<Result> useCoupon(@Valid @RequestBody CouponUseRequest request) {
        couponService.useCoupon(request);
        return Results.ok();
    }

    @PostMapping("/internal/rollback")
    public ResponseEntity<Result> rollbackCoupon(@Valid @RequestBody CouponRollbackRequest request) {
        couponService.rollbackCoupon(request);
        return Results.ok();
    }
}
