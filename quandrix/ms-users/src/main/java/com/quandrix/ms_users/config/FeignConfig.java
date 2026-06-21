package com.quandrix.ms_users.config;

import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableFeignClients(basePackages = "com.quandrix.ms_users.client")
public class FeignConfig {
}