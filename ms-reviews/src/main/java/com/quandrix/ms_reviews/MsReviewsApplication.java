package com.quandrix.ms_reviews;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class MsReviewsApplication {
    public static void main(String[] args) {
        SpringApplication.run(MsReviewsApplication.class, args);
    }
}