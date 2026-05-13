package com.quandrix.ms_reports.config;

import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableFeignClients(basePackages = "com.quandrix.ms_reports.client")
public class FeignConfig {
}