package com.quandrix.ms_orders.dto;

import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;

@Getter
@Setter
public class ListingResponse {
    private Long id;
    private Long sellerId;
    private String scryfallId;
    private BigDecimal price;
    private String status;
    private Integer quantity;
}