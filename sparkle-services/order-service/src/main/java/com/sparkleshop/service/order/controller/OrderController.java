package com.sparkleshop.service.order.controller;

import com.sparkleshop.common.core.model.Result;
import com.sparkleshop.common.web.util.Results;
import com.sparkleshop.service.order.dto.MockPayRequest;
import com.sparkleshop.service.order.dto.OrderListQueryRequest;
import com.sparkleshop.service.order.dto.SubmitOrderRequest;
import com.sparkleshop.service.order.service.OrderService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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

    @GetMapping("/{orderId}/pay/token")
    public ResponseEntity<Result> generatePayToken(@PathVariable @Min(1) Long orderId) {
        return Results.ok(orderService.generatePayToken(orderId));
    }

    @PostMapping("/{orderId}/mock-pay")
    public ResponseEntity<Result> mockPay(@PathVariable @Min(1) Long orderId,
                                          @Valid @RequestBody MockPayRequest request) {
        orderService.mockPay(orderId, request);
        return Results.ok();
    }

    @PostMapping("/{orderId}/cancel")
    public ResponseEntity<Result> cancelOrder(@PathVariable @Min(1) Long orderId) {
        orderService.cancelOrder(orderId);
        return Results.ok();
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<Result> getOrderDetail(@PathVariable @Min(1) Long orderId) {
        return Results.ok(orderService.getOrderDetail(orderId));
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
