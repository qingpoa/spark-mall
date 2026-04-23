package com.sparkleshop.service.user.controller;

import com.sparkleshop.common.core.model.Result;
import com.sparkleshop.common.web.util.Results;
import com.sparkleshop.service.user.dto.address.AddressDetailRequest;
import com.sparkleshop.service.user.service.UserAddressService;
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
@RequestMapping({"", "/user"})
public class UserAddressInternalController {

    private final UserAddressService userAddressService;

    @PostMapping("/internal/address/detail")
    public ResponseEntity<Result> getAddressDetail(@Valid @RequestBody AddressDetailRequest request) {
        return Results.ok(userAddressService.getAddressDetail(request.getUserId(), request.getAddressId()));
    }
}
