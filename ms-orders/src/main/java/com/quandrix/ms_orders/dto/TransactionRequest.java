package com.quandrix.ms_orders.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import java.math.BigDecimal;

@Getter
@AllArgsConstructor
public class TransactionRequest {
    private Long orderId;
    private Long buyerId;
    private Long sellerId;
    private String scryfallId;
    private BigDecimal amount;
}