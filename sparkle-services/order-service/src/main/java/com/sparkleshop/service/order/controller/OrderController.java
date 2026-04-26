package com.sparkleshop.service.order.controller;

import com.sparkleshop.common.core.model.Result;
import com.sparkleshop.common.web.util.Results;
import com.sparkleshop.service.order.dto.OrderListQueryRequest;
import com.sparkleshop.service.order.dto.SubmitOrderRequest;
import com.sparkleshop.service.order.service.OrderService;
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
@RequiredArgsConstructor
@RequestMapping("/order")
public class OrderController {

    private final OrderService orderService;

    @GetMapping("/submit/token")
    public ResponseEntity<Result> generateSubmitToken() {
        return Results.ok(orderService.generateSubmitToken());
    }

    @GetMapping("/list")
    public ResponseEntity<Result> getOrderList(@Valid OrderListQueryRequest request) {
        return Results.ok(orderService.getOrderList(request));
    }

    @PostMapping("/submit")
    public ResponseEntity<Result> submitOrder(@Valid @RequestBody SubmitOrderRequest request) {
        return Results.created(orderService.submitOrder(request));
    }
}
