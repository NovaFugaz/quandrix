package com.quandrix.ms_listings.client;

import com.quandrix.ms_listings.dto.UserResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "ms-users")
public interface UserClient {

    @GetMapping("/users/{userId}")
    UserResponse getUser(@PathVariable Long userId);
}