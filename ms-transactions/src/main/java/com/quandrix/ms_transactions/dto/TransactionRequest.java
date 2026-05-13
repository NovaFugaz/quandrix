package com.quandrix.ms_transactions.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;

@Getter
@Setter
public class TransactionRequest {

    @NotNull(message = "El orderId es obligatorio")
    private Long orderId;

    @NotNull(message = "El buyerId es obligatorio")
    private Long buyerId;

    @NotNull(message = "El sellerId es obligatorio")
    private Long sellerId;

    @NotBlank(message = "El scryfallId es obligatorio")
    private String scryfallId;

    @NotNull(message = "El monto es obligatorio")
    @DecimalMin(value = "0.01", message = "El monto debe ser mayor a 0")
    private BigDecimal amount;
}