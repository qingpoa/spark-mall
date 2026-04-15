package com.sparkleshop.service.user.controller;

import com.sparkleshop.common.core.model.Result;
import com.sparkleshop.common.security.annotation.RequireLogin;
import com.sparkleshop.common.web.util.Results;
import com.sparkleshop.service.user.dto.address.AddressCreateRequest;
import com.sparkleshop.service.user.dto.address.AddressUpdateRequest;
import com.sparkleshop.service.user.service.UserAddressService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/user/address")
public class UserAddressController {

    private final UserAddressService userAddressService;

    @RequireLogin
    @GetMapping("/list")
    public ResponseEntity<Result> listCurrentUserAddresses() {
        return Results.ok(userAddressService.listCurrentUserAddresses());
    }

    @RequireLogin
    @PostMapping
    public ResponseEntity<Result> createCurrentUserAddress(@Valid @RequestBody AddressCreateRequest request) {
        return Results.created(userAddressService.createCurrentUserAddress(request));
    }

    @RequireLogin
    @PutMapping("/{id}")
    public ResponseEntity<Result> updateCurrentUserAddress(@PathVariable("id") Long addressId,
                                                           @Valid @RequestBody AddressUpdateRequest request) {
        userAddressService.updateCurrentUserAddress(addressId, request);
        return Results.ok();
    }

    @RequireLogin
    @DeleteMapping("/{id}")
    public ResponseEntity<Result> deleteCurrentUserAddress(@PathVariable("id") Long addressId) {
        userAddressService.deleteCurrentUserAddress(addressId);
        return Results.ok();
    }

    @RequireLogin
    @PutMapping("/{id}/default")
    public ResponseEntity<Result> setCurrentUserDefaultAddress(@PathVariable("id") Long addressId) {
        userAddressService.setCurrentUserDefaultAddress(addressId);
        return Results.ok();
    }
}
