package com.quandrix.ms_payments.controller;

import com.quandrix.ms_payments.dto.PaymentRequest;
import com.quandrix.ms_payments.dto.PaymentResponse;
import com.quandrix.ms_payments.service.PaymentService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/payments")
public class PaymentController {

    private static final Logger log = LoggerFactory.getLogger(PaymentController.class);

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/process")
    public ResponseEntity<PaymentResponse> process(
            @Valid @RequestBody PaymentRequest request) {
        log.info("POST /payments/process orderId={}", request.getOrderId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(paymentService.process(request));
    }

    @GetMapping("/order/{orderId}")
    public ResponseEntity<PaymentResponse> getByOrderId(
            @PathVariable Long orderId) {
        log.info("GET /payments/order/{}", orderId);
        return ResponseEntity.ok(paymentService.getByOrderId(orderId));
    }
}