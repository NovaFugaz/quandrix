package com.quandrix.ms_listings.controller;

import com.quandrix.ms_listings.dto.ListingRequest;
import com.quandrix.ms_listings.dto.ListingResponse;
import com.quandrix.ms_listings.service.ListingService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/listings")
public class ListingController {

    private static final Logger log = LoggerFactory.getLogger(ListingController.class);

    private final ListingService listingService;

    public ListingController(ListingService listingService) {
        this.listingService = listingService;
    }

    @PostMapping
    public ResponseEntity<ListingResponse> create(
            @Valid @RequestBody ListingRequest request) {
        log.info("POST /listings - sellerId={}", request.getSellerId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(listingService.create(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ListingResponse> getById(@PathVariable Long id) {
        log.info("GET /listings/{}", id);
        return ResponseEntity.ok(listingService.getById(id));
    }

    @GetMapping
    public ResponseEntity<List<ListingResponse>> getAllActive() {
        log.info("GET /listings");
        return ResponseEntity.ok(listingService.getAllActive());
    }

    @GetMapping("/seller/{sellerId}")
    public ResponseEntity<List<ListingResponse>> getBySeller(
            @PathVariable Long sellerId) {
        log.info("GET /listings/seller/{}", sellerId);
        return ResponseEntity.ok(listingService.getBySeller(sellerId));
    }

    @GetMapping("/card/{scryfallId}")
    public ResponseEntity<List<ListingResponse>> getByCard(
            @PathVariable String scryfallId) {
        log.info("GET /listings/card/{}", scryfallId);
        return ResponseEntity.ok(listingService.getByCard(scryfallId));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ListingResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody ListingRequest request) {
        log.info("PUT /listings/{}", id);
        return ResponseEntity.ok(listingService.update(id, request));
    }

    @PatchMapping("/{id}/withdraw")
    public ResponseEntity<ListingResponse> withdraw(
            @PathVariable Long id,
            @RequestParam Long sellerId) {
        log.info("PATCH /listings/{}/withdraw - sellerId={}", id, sellerId);
        return ResponseEntity.ok(listingService.withdraw(id, sellerId));
    }

    // Endpoint interno para ms-orders
    @PatchMapping("/{id}/sold")
    public ResponseEntity<Void> markAsSold(@PathVariable Long id) {
        log.info("PATCH /listings/{}/sold", id);
        listingService.markAsSold(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        log.info("DELETE /listings/{}", id);
        listingService.delete(id);
        return ResponseEntity.noContent().build();
    }
}