package com.quandrix.ms_catalog.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Bean
    public WebClient scryfallWebClient() {
        ExchangeStrategies strategies = ExchangeStrategies.builder()
                .codecs(configurer -> configurer
                        .defaultCodecs()
                        .maxInMemorySize(2 * 1024 * 1024)) // 2 MB
                .build();

        return WebClient.builder()
                .baseUrl("https://api.scryfall.com")
                .defaultHeader("User-Agent", "Quandrix-MTG-Marketplace/1.0")
                .defaultHeader("Accept", "application/json")
                .exchangeStrategies(strategies) // Aplicamos la estrategia
                .build();
    }
}