package com.quandrix.ms_orders.config;

import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableFeignClients(basePackages = "com.quandrix.ms_orders.client")
public class FeignConfig {
}