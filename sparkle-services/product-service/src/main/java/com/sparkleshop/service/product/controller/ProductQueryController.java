package com.sparkleshop.service.product.controller;

import com.sparkleshop.common.core.model.Result;
import com.sparkleshop.common.web.util.Results;
import com.sparkleshop.service.product.dto.ProductPageQueryDTO;
import com.sparkleshop.service.product.service.ProductQueryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/product")
public class ProductQueryController {

    private final ProductQueryService productQueryService;

    @GetMapping("/list")
    public ResponseEntity<Result> getProductPage(@Valid ProductPageQueryDTO queryDTO) {
        return Results.ok(productQueryService.getProductPage(queryDTO));
    }

    @GetMapping("/hot")
    public ResponseEntity<Result> getHotProducts(@RequestParam(value = "limit", required = false) Integer limit) {
        return Results.ok(productQueryService.getHotProducts(limit));
    }

    @GetMapping("/{skuId}")
    public ResponseEntity<Result> getProductDetail(@PathVariable("skuId") Long skuId) {
        return Results.ok(productQueryService.getProductDetail(skuId));
    }
}
