package com.quandrix.ms_reviews.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ReviewResponse {
    private Long id;
    private Long reviewerId;
    private Long sellerId;
    private Integer rating;
    private String comment;
    private LocalDateTime createdAt;
}
