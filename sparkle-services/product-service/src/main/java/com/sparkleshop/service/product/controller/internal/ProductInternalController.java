package com.sparkleshop.service.product.controller.internal;

import com.sparkleshop.common.core.model.Result;
import com.sparkleshop.common.web.util.Results;
import com.sparkleshop.service.product.dto.internal.ProductSkuSnapshotQueryRequest;
import com.sparkleshop.service.product.service.ProductQueryService;
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
@RequestMapping({"", "/product"})
public class ProductInternalController {

    private final ProductQueryService productQueryService;

    @PostMapping("/internal/sku/snapshot")
    public ResponseEntity<Result> getSkuSnapshots(@Valid @RequestBody ProductSkuSnapshotQueryRequest request) {
        return Results.ok(productQueryService.getSkuSnapshots(request.getSkuIds()));
    }
}
