package com.quandrix.ms_orders.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ListingResponse {
    private Long id;
    private Long sellerId;
    private String scryfallId;
    private Long price;
    private String status;
    private Integer quantity;
}