package com.quandrix.ms_orders.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import java.math.BigDecimal;

@Getter
@AllArgsConstructor
public class PaymentProcessRequest {
    private Long orderId;
    private BigDecimal amount;
    private String method;
}