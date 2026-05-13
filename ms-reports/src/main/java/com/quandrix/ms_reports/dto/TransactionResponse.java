package com.quandrix.ms_reports.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class TransactionResponse {
    private Long id;
    private Long orderId;
    private Long buyerId;
    private Long sellerId;
    private String scryfallId;
    private BigDecimal amount;
    private LocalDateTime completedAt;
}
