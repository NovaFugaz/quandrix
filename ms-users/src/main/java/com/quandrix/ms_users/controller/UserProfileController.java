package com.quandrix.ms_users.controller;

import com.quandrix.ms_users.dto.UserProfileRequest;
import com.quandrix.ms_users.dto.UserProfileResponse;
import com.quandrix.ms_users.service.UserProfileService;
import jakarta.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/users")
public class UserProfileController {

    private final UserProfileService service;
    private static final Logger log = LoggerFactory.getLogger(UserProfileController.class);

    public UserProfileController(UserProfileService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<UserProfileResponse> create(
            @Valid @RequestBody UserProfileRequest request) {
        log.info("POST /users userId={}", request.getUserId());
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
    }

    @GetMapping("/{userId}")
    public ResponseEntity<UserProfileResponse> getByUserId(@PathVariable Long userId) {
        log.info("GET /users/{}", userId);
        return ResponseEntity.ok(service.getByUserId(userId));
    }

    @GetMapping
    public ResponseEntity<List<UserProfileResponse>> getAll() {
        log.info("GET /users");
        return ResponseEntity.ok(service.getAll());
    }

    @PutMapping("/{userId}")
    public ResponseEntity<UserProfileResponse> update(
            @PathVariable Long userId,
            @Valid @RequestBody UserProfileRequest request) {
        log.info("PUT /users/{}", userId);
        return ResponseEntity.ok(service.update(userId, request));
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> delete(@PathVariable Long userId) {
        log.info("DELETE /users/{}", userId);
        service.delete(userId);
        return ResponseEntity.noContent().build();
    }
}