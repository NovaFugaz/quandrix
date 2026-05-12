package com.quandrix.ms_orders.controller;

import com.quandrix.ms_orders.dto.OrderRequest;
import com.quandrix.ms_orders.dto.OrderResponse;
import com.quandrix.ms_orders.service.OrderService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/orders")
public class OrderController {

    private static final Logger log = LoggerFactory.getLogger(OrderController.class);

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public ResponseEntity<OrderResponse> create(
            @Valid @RequestBody OrderRequest request) {
        log.info("POST /orders buyerId={}", request.getBuyerId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(orderService.create(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> getById(@PathVariable Long id) {
        log.info("GET /orders/{}", id);
        return ResponseEntity.ok(orderService.getById(id));
    }

    @GetMapping("/buyer/{buyerId}")
    public ResponseEntity<List<OrderResponse>> getByBuyer(
            @PathVariable Long buyerId) {
        log.info("GET /orders/buyer/{}", buyerId);
        return ResponseEntity.ok(orderService.getByBuyer(buyerId));
    }

    @GetMapping("/seller/{sellerId}")
    public ResponseEntity<List<OrderResponse>> getBySeller(
            @PathVariable Long sellerId) {
        log.info("GET /orders/seller/{}", sellerId);
        return ResponseEntity.ok(orderService.getBySeller(sellerId));
    }

    @PatchMapping("/{id}/cancel")
    public ResponseEntity<OrderResponse> cancel(
            @PathVariable Long id,
            @RequestParam Long buyerId) {
        log.info("PATCH /orders/{}/cancel buyerId={}", id, buyerId);
        return ResponseEntity.ok(orderService.cancel(id, buyerId));
    }
}