package com.quandrix.ms_transactions.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "")
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
    @Min(value = 1, message = "El monto mínimo es 1")
    private Long amount;
}