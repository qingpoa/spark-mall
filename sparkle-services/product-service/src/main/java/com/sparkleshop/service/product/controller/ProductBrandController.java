package com.sparkleshop.service.product.controller;

import com.sparkleshop.common.core.model.Result;
import com.sparkleshop.common.web.util.Results;
import com.sparkleshop.service.product.service.BrandService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/product/brand")
public class ProductBrandController {

    private final BrandService brandService;

    @GetMapping("/list")
    public ResponseEntity<Result> getBrandList(@RequestParam(value = "keyword", required = false) String keyword) {
        return Results.ok(brandService.getEnabledBrandList(keyword));
    }
}
