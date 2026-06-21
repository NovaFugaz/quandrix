package com.quandrix.ms_reviews.config;

import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableFeignClients(basePackages = "com.quandrix.ms_reviews.client")
public class FeignConfig {
}