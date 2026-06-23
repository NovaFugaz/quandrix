package com.quandrix.ms_transactions.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;

@Getter
@AllArgsConstructor
@Schema(title = "Transaction Response", description = "Respuesta de una transacción")
public class TransactionResponse {

    @Schema(description = "Id de la transacción", example = "1")
    private Long id;

    @Schema(description = "Id de la orden", example = "1")
    private Long orderId;

    @Schema(description = "Id del comprador", example = "1")
    private Long buyerId;

    @Schema(description = "Id del vendedor", example = "1")
    private Long sellerId;

    @Schema(description = "Id único de la carta", example = "bd8fa8c8-7e1c-4f5d-a6d3-123456789abc")
    private String scryfallId;

    @Schema(description = "Monto a pagar", example = "5000")
    private Long amount;

    @Schema(description = "Fecha de transacción completada", example = "2026-06-12T14:30:00")
    private LocalDateTime completedAt;
}