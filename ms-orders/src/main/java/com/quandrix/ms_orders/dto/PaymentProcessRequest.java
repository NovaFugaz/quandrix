package com.quandrix.ms_orders.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class PaymentProcessRequest {
    private Long orderId;
    private Long amount;
    private String method;
}