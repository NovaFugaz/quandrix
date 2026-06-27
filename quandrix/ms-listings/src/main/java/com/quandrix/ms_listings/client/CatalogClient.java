package com.quandrix.ms_listings.client;

import com.quandrix.ms_listings.dto.CardResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "ms-catalog")
public interface CatalogClient {

    @GetMapping("/catalog/find")
    CardResponse findByNameAndSet(
            @RequestParam String name,
            @RequestParam(required = false) String setCode);
}