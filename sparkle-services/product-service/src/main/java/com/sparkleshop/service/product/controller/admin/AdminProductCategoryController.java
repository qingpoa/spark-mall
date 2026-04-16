package com.sparkleshop.service.product.controller.admin;

import com.sparkleshop.common.core.model.Result;
import com.sparkleshop.common.security.annotation.RequireLogin;
import com.sparkleshop.common.web.util.Results;
import com.sparkleshop.service.product.dto.admin.AdminCategoryCreateRequest;
import com.sparkleshop.service.product.service.CategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequireLogin
@RequiredArgsConstructor
@RequestMapping("/admin/product/category")
public class AdminProductCategoryController {

    private final CategoryService categoryService;

    @GetMapping("/list")
    public ResponseEntity<Result> getCategoryList() {
        return Results.ok(categoryService.getAdminCategoryList());
    }

    @PostMapping
    public ResponseEntity<Result> createCategory(@Valid @RequestBody AdminCategoryCreateRequest request) {
        return Results.created(categoryService.createCategory(request));
    }
}
