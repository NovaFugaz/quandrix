package com.quandrix.ms_reviews.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class SellerRatingResponse {
    private Long sellerId;
    private Double averageRating;
    private Long totalReviews;
}
