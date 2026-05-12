package com.quandrix.ms_payments.dto;

import com.quandrix.ms_payments.model.PaymentMethod;
import com.quandrix.ms_payments.model.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class PaymentResponse {
    private Long id;
    private Long orderId;
    private BigDecimal amount;
    private PaymentMethod method;
    private PaymentStatus status;
    private LocalDateTime processedAt;
    private LocalDateTime createdAt;
}