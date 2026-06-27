package com.quandrix.ms_users.client;

import com.quandrix.ms_users.dto.TokenValidationResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "ms-auth")
public interface AuthClient {

    @GetMapping("/auth/validate")
    TokenValidationResponse validate(@RequestHeader("Authorization") String authHeader);
}