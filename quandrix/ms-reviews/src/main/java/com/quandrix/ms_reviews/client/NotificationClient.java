package com.quandrix.ms_reviews.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import com.quandrix.ms_reviews.dto.NotificationRequest;

@FeignClient(name = "ms-notifications")
public interface NotificationClient {

    @PostMapping("/notifications")
    void send(@RequestBody NotificationRequest request);
}
