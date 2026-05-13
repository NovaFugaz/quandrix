package com.quandrix.ms_reviews.controller;

import com.quandrix.ms_reviews.dto.ReviewRequest;
import com.quandrix.ms_reviews.dto.ReviewResponse;
import com.quandrix.ms_reviews.dto.SellerRatingResponse;
import com.quandrix.ms_reviews.service.ReviewService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/reviews")
public class ReviewController {

    private static final Logger log = LoggerFactory.getLogger(ReviewController.class);

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @PostMapping
    public ResponseEntity<ReviewResponse> create(
            @Valid @RequestBody ReviewRequest request) {
        log.info("POST /reviews reviewerId={} sellerId={}",
                request.getReviewerId(), request.getSellerId());
        return ResponseEntity.status(HttpStatus.CREATED).body(reviewService.create(request));
    }

    @GetMapping("/seller/{sellerId}")
    public ResponseEntity<List<ReviewResponse>> getBySeller(
            @PathVariable Long sellerId) {
        log.info("GET /reviews/seller/{}", sellerId);
        return ResponseEntity.ok(reviewService.getBySeller(sellerId));
    }

    @GetMapping("/seller/{sellerId}/rating")
    public ResponseEntity<SellerRatingResponse> getSellerRating(
            @PathVariable Long sellerId) {
        log.info("GET /reviews/seller/{}/rating", sellerId);
        return ResponseEntity.ok(reviewService.getSellerRating(sellerId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ReviewResponse> getById(@PathVariable Long id) {
        log.info("GET /reviews/{}", id);
        return ResponseEntity.ok(reviewService.getById(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        log.info("DELETE /reviews/{}", id);
        reviewService.delete(id);
        return ResponseEntity.noContent().build();
    }
}