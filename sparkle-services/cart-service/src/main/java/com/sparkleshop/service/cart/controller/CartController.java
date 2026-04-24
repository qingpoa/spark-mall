package com.sparkleshop.service.cart.controller;

import com.sparkleshop.common.core.model.Result;
import com.sparkleshop.common.web.util.Results;
import com.sparkleshop.service.cart.dto.CartAddRequest;
import com.sparkleshop.service.cart.dto.CartSelectAllRequest;
import com.sparkleshop.service.cart.dto.CartSelectRequest;
import com.sparkleshop.service.cart.dto.CartUpdateRequest;
import com.sparkleshop.service.cart.service.CartService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    @GetMapping("/list")
    public ResponseEntity<Result> getCartList() {
        return Results.ok(cartService.getCartList());
    }

    @PostMapping("/add")
    public ResponseEntity<Result> addCart(@Valid @RequestBody CartAddRequest cartAddReqVO) {
        return Results.ok(cartService.addCart(cartAddReqVO));
    }

    @PutMapping("/update")
    public ResponseEntity<Result> updateCart(@Valid @RequestBody CartUpdateRequest cartUpdateReqVO) {
        cartService.updateCart(cartUpdateReqVO);
        return Results.ok();
    }

    @DeleteMapping("/{skuId}")
    public ResponseEntity<Result> deleteCartItem(@PathVariable Long skuId) {
        cartService.deleteCartItem(skuId);
        return Results.ok();
    }

    @PutMapping("/select")
    public ResponseEntity<Result> selectCartItem(@Valid @RequestBody CartSelectRequest cartSelectReqVO) {
        cartService.selectCartItem(cartSelectReqVO);
        return Results.ok();
    }

    @PutMapping("/select/all")
    public ResponseEntity<Result> selectAllCartItem(@Valid @RequestBody CartSelectAllRequest select) {
        cartService.selectAllCartItem(select);
        return Results.ok();
    }
}
