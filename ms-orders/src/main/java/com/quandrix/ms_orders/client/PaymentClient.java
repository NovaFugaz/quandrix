package com.quandrix.ms_orders.client;

import com.quandrix.ms_orders.dto.PaymentRequest;
import com.quandrix.ms_orders.dto.PaymentResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "ms-payments")
public interface PaymentClient {

    @PostMapping("/payments/process")
    PaymentResponse process(@RequestBody PaymentRequest request);
}