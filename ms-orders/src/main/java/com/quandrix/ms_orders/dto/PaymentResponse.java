package com.quandrix.ms_orders.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PaymentResponse {
    private Long id;
    private Long orderId;
    private String status;
}