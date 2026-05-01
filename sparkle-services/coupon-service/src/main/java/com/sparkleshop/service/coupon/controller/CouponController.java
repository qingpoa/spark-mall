package com.sparkleshop.service.coupon.controller;

import com.sparkleshop.common.core.model.Result;
import com.sparkleshop.common.web.util.Results;
import com.sparkleshop.service.coupon.dto.CouponListQueryDTO;
import com.sparkleshop.service.coupon.dto.CouponReceiveRequest;
import com.sparkleshop.service.coupon.dto.CouponValidateQueryDTO;
import com.sparkleshop.service.coupon.dto.MyCouponQueryDTO;
import com.sparkleshop.service.coupon.service.CouponService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/coupon")
public class CouponController {

    private final CouponService couponService;

    @PostMapping("/receive")
    public ResponseEntity<Result> receiveCoupon(@Valid @RequestBody CouponReceiveRequest request) {
        return Results.created(couponService.receiveCoupon(request));
    }

    @GetMapping("/list")
    public ResponseEntity<Result> getReceivableCouponPage(@Valid CouponListQueryDTO queryDTO) {
        return Results.ok(couponService.getReceivableCouponPage(queryDTO));
    }

    @GetMapping("/my")
    public ResponseEntity<Result> getCurrentUserCouponPage(@Valid MyCouponQueryDTO queryDTO) {
        return Results.ok(couponService.getCurrentUserCouponPage(queryDTO));
    }

    @GetMapping("/{couponId}/validate")
    public ResponseEntity<Result> validateCurrentUserCoupon(@PathVariable("couponId") @Min(1) Long couponId,
                                                            @Valid CouponValidateQueryDTO queryDTO) {
        return Results.ok(couponService.validateCurrentUserCoupon(couponId, queryDTO));
    }
}
