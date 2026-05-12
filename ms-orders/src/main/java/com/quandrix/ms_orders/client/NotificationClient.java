package com.quandrix.ms_orders.client;

import com.quandrix.ms_orders.dto.NotificationRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "ms-notifications")
public interface NotificationClient {

    @PostMapping("/notifications")
    void send(@RequestBody NotificationRequest request);
}