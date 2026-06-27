package com.quandrix.ms_transactions.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(title = "Transaction Request", description = "Datos necesarios para crear la transacción")
public class TransactionRequest {

    @NotNull(message = "El orderId es obligatorio")
    @Schema(description = "Id de la orden", example = "1")
    private Long orderId;

    @NotNull(message = "El buyerId es obligatorio")
    @Schema(description = "Id del comprador", example = "1")
    private Long buyerId;

    @NotNull(message = "El sellerId es obligatorio")
    @Schema(description = "Id del vendedor", example = "1")
    private Long sellerId;

    @NotBlank(message = "El scryfallId es obligatorio")
    @Schema(description = "Id único de la carta", example = "bd8fa8c8-7e1c-4f5d-a6d3-123456789abc")
    private String scryfallId;

    @NotNull(message = "El monto es obligatorio")
    @Min(value = 1, message = "El monto mínimo es 1")
    @Schema(description = "Monto a pagar", example = "5000")
    private Long amount;
}