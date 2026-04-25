package com.sparkleshop.service.stock.controller.internal;

import com.sparkleshop.common.core.model.Result;
import com.sparkleshop.common.web.util.Results;
import com.sparkleshop.service.stock.dto.internal.StockLockRequest;
import com.sparkleshop.service.stock.service.StockService;
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
@RequestMapping({"", "/stock"})
public class StockInternalController {

    private final StockService stockService;

    @PostMapping("/internal/lock")
    public ResponseEntity<Result> lockStock(@Valid @RequestBody StockLockRequest request) {
        stockService.lockStock(request);
        return Results.ok();
    }
}
