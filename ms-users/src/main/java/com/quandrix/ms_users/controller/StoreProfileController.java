package com.quandrix.ms_users.controller;

import com.quandrix.ms_users.dto.StoreProfileRequest;
import com.quandrix.ms_users.dto.StoreProfileResponse;
import com.quandrix.ms_users.service.StoreProfileService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/stores")
public class StoreProfileController {

    private final StoreProfileService service;

    public StoreProfileController(StoreProfileService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<StoreProfileResponse> create(@Valid @RequestBody StoreProfileRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
    }

    @GetMapping("/{userId}")
    public ResponseEntity<StoreProfileResponse> getByUserId(@PathVariable Long userId) {
        return ResponseEntity.ok(service.getByUserId(userId));
    }

    @GetMapping
    public ResponseEntity<List<StoreProfileResponse>> getAll() {
        return ResponseEntity.ok(service.getAll());
    }

    @PutMapping("/{userId}")
    public ResponseEntity<StoreProfileResponse> update(
            @PathVariable Long userId,
            @Valid @RequestBody StoreProfileRequest request) {
        return ResponseEntity.ok(service.update(userId, request));
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> delete(@PathVariable Long userId) {
        service.delete(userId);
        return ResponseEntity.noContent().build();
    }
}