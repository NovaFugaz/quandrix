package com.quandrix.ms_orders.client;

import com.quandrix.ms_orders.dto.PaymentProcessRequest;
import com.quandrix.ms_orders.dto.PaymentResponse;
import com.quandrix.ms_orders.dto.TransactionRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.math.BigDecimal;

@FeignClient(name = "ms-payments")
public interface PaymentClient {

    @PostMapping("/payments/process")
    PaymentResponse process(@RequestBody PaymentProcessRequest request);
}