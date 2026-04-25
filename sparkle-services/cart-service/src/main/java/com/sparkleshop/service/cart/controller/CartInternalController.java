package com.sparkleshop.service.cart.controller;

import com.sparkleshop.common.core.model.Result;
import com.sparkleshop.common.web.util.Results;
import com.sparkleshop.service.cart.dto.internal.CartClearItemsRequest;
import com.sparkleshop.service.cart.service.CartService;
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
@RequestMapping({"", "/cart"})
public class CartInternalController {

    private final CartService cartService;

    @PostMapping("/internal/clear")
    public ResponseEntity<Result> clearCheckedItems(@Valid @RequestBody CartClearItemsRequest request) {
        cartService.clearCheckedItems(request.getUserId(), request.getSkuIds());
        return Results.ok();
    }
}
