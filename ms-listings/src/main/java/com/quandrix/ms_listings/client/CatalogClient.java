package com.quandrix.ms_listings.client;

import com.quandrix.ms_listings.dto.CardResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "ms-catalog")
public interface CatalogClient {

    @GetMapping("/catalog/{scryfallId}")
    CardResponse getCard(@PathVariable String scryfallId);
}