package com.quandrix.ms_listings.dto;

import com.quandrix.ms_listings.model.CardCondition;
import com.quandrix.ms_listings.model.ListingStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class ListingResponse {
    private Long id;
    private Long sellerId;
    private String scryfallId;
    private CardCondition condition;
    private BigDecimal price;
    private Integer quantity;
    private ListingStatus status;
    private LocalDateTime createdAt;
}