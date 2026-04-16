package com.sparkleshop.service.product.controller.admin;

import com.sparkleshop.common.core.model.Result;
import com.sparkleshop.common.security.annotation.RequireLogin;
import com.sparkleshop.common.web.util.Results;
import com.sparkleshop.service.product.dto.admin.AdminSpuCreateRequest;
import com.sparkleshop.service.product.dto.admin.AdminSpuPageQueryDTO;
import com.sparkleshop.service.product.service.ProductAdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequireLogin
@RequiredArgsConstructor
@RequestMapping("/admin/product/spu")
public class AdminProductSpuController {

    private final ProductAdminService productAdminService;

    @GetMapping("/list")
    public ResponseEntity<Result> getSpuPage(@Valid AdminSpuPageQueryDTO queryDTO) {
        return Results.ok(productAdminService.getAdminSpuPage(queryDTO));
    }

    @PostMapping
    public ResponseEntity<Result> createSpu(@Valid @RequestBody AdminSpuCreateRequest request) {
        return Results.created(productAdminService.createSpu(request));
    }
}
