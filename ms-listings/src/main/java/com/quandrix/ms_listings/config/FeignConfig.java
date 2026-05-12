package com.quandrix.ms_listings.config;

import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableFeignClients(basePackages = "com.quandrix.ms_listings.client")
public class FeignConfig {
}