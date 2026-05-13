package com.quandrix.ms_transactions;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class MsTransactionsApplication {
	public static void main(String[] args) {
		SpringApplication.run(MsTransactionsApplication.class, args);
	}
}