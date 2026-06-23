package com.quandrix.ms_orders.dto;

import com.quandrix.ms_orders.model.OrderStatus;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
@Schema(title = "Order Response", description = "Respuesta del contenido de una orden")
public class OrderResponse {

    @Schema(description = "Id orden de compra", example = "1")
    private Long id;

    @Schema(description = "Id del comprador", example = "1")
    private Long buyerId;

    @Schema(description = "Id de la publicación", example = "1")
    private Long listingId;

    @Schema(description = "Id del vendedor", example = "1")
    private Long sellerId;

    @Schema(description = "Monto a pagar", example = "5000")
    private Long amount;

    @Schema(description = "Estado de la orden", example = "PENDING", allowableValues = {"PENDING", "CONFIRMED", "COMPLETED", "CANCELLED"})
    private OrderStatus status;

    @Schema(description = "Metodo para pagar", example = "CREDIT_CARD", allowableValues = {"DEBIT_CARD", "BANK_TRANSFER", "CASH", "CREDIT_CARD"})
    private String paymentMethod;

    @Schema(description = "Fecha de creación de la orden de compra", example = "2025-06-13T10:30:00")
    private LocalDateTime createdAt;

    @Schema(description = "Fecha de ultima actualización", example = "2025-06-13T10:30:00")
    private LocalDateTime updatedAt;
}