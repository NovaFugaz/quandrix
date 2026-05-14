package com.quandrix.ms_users.controller;

import com.quandrix.ms_users.dto.StoreProfileRequest;
import com.quandrix.ms_users.dto.StoreProfileResponse;
import com.quandrix.ms_users.service.StoreProfileService;
import jakarta.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/stores")
public class StoreProfileController {

    private final StoreProfileService service;
    private static final Logger log = LoggerFactory.getLogger(StoreProfileController.class);

    public StoreProfileController(StoreProfileService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<StoreProfileResponse> create(
            @Valid @RequestBody StoreProfileRequest request) {
        log.info("POST /stores userId={} storeName='{}'",
                request.getUserId(), request.getStoreName());
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
    }

    @GetMapping("/{userId}")
    public ResponseEntity<StoreProfileResponse> getByUserId(@PathVariable Long userId) {
        log.info("GET /stores/{}", userId);
        return ResponseEntity.ok(service.getByUserId(userId));
    }

    @GetMapping
    public ResponseEntity<List<StoreProfileResponse>> getAll() {
        log.info("GET /stores");
        return ResponseEntity.ok(service.getAll());
    }

    @PutMapping("/{userId}")
    public ResponseEntity<StoreProfileResponse> update(
            @PathVariable Long userId,
            @Valid @RequestBody StoreProfileRequest request) {
        log.info("PUT /stores/{}", userId);
        return ResponseEntity.ok(service.update(userId, request));
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> delete(@PathVariable Long userId) {
        log.info("DELETE /stores/{}", userId);
        service.delete(userId);
        return ResponseEntity.noContent().build();
    }
}