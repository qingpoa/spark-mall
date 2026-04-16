package com.sparkleshop.service.product.controller;

import com.sparkleshop.common.core.model.Result;
import com.sparkleshop.common.web.util.Results;
import com.sparkleshop.service.product.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/product/category")
public class ProductCategoryController {

    private final CategoryService categoryService;

    @GetMapping("/tree")
    public ResponseEntity<Result> getCategoryTree() {
        return Results.ok(categoryService.getCategoryTree());
    }
}
