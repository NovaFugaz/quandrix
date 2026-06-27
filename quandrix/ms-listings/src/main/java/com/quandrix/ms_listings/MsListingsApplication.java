package com.quandrix.ms_listings;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class MsListingsApplication {
    public static void main(String[] args) {
        SpringApplication.run(MsListingsApplication.class, args);
    }
}