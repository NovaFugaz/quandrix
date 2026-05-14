package com.quandrix.ms_transactions.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class TransactionResponse {
    private Long id;
    private Long orderId;
    private Long buyerId;
    private Long sellerId;
    private String scryfallId;
    private Long amount;
    private LocalDateTime completedAt;
}