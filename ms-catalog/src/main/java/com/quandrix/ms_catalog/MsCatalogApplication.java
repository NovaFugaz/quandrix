package com.quandrix.ms_catalog;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class MsCatalogApplication {
    public static void main(String[] args) {
        SpringApplication.run(MsCatalogApplication.class, args);
    }
}