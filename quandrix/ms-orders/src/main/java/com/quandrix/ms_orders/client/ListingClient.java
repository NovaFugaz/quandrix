package com.quandrix.ms_orders.client;

import com.quandrix.ms_orders.dto.ListingResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "ms-listings")
public interface ListingClient {

    @GetMapping("/listings/{id}")
    ListingResponse getById(@PathVariable Long id);

    @PatchMapping("/listings/{id}/sold")
    void markAsSold(@PathVariable Long id);
}