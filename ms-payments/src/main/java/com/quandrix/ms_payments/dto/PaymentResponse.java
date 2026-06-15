package com.quandrix.ms_payments.dto;

import com.quandrix.ms_payments.model.PaymentMethod;
import com.quandrix.ms_payments.model.PaymentStatus;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class PaymentResponse {

    @Schema(description = "Id del pago", example = "1")
    private Long id;

    @Schema(description = "Id de la orden", example = "1")
    private Long orderId;

    @Schema(description = "Monto a pagar", example = "5000")
    private Long amount;

    @Schema(description = "Metodo para pagar", example = "CREDIT_CARD /{DEBIT_CARD, BANK_TRANSFER, CASH}")
    private PaymentMethod method;

    @Schema(description = "Estado del pago", example = "PENDING /{APPROVED, REJECTED}")
    private PaymentStatus status;

    @Schema(description = "Fecha de procesamiento del pago", example = "2026-06-12T14:30:00")
    private LocalDateTime processedAt;

    @Schema(description = "Fecha de creación de pago", example = "2026-06-12T14:30:00")
    private LocalDateTime createdAt;
}