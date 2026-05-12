package com.quandrix.ms_orders.client;

import com.quandrix.ms_orders.dto.TransactionRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "ms-transactions")
public interface TransactionClient {

    @PostMapping("/transactions")
    void register(@RequestBody TransactionRequest request);
}