package com.quandrix.ms_orders.dto;

import com.quandrix.ms_orders.model.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class OrderResponse {
    private Long id;
    private Long buyerId;
    private Long listingId;
    private Long sellerId;
    private BigDecimal amount;
    private OrderStatus status;
    private String paymentMethod;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}