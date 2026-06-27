package com.quandrix.ms_orders.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(title = "Order Request", description = "Datos necesarios para generar una orden")
public class OrderRequest {

    @NotNull(message = "El buyerId es obligatorio")
    @Schema(description = "Id del comprador", example = "1")
    private Long buyerId;

    @NotNull(message = "El listingId es obligatorio")
    @Schema(description = "Id de la publicación", example = "1")
    private Long listingId;

    @NotBlank(message = "El método de pago es obligatorio")
    @Schema(description = "Metodo para pagar", example = "CREDIT_CARD", allowableValues = {"DEBIT_CARD", "BANK_TRANSFER", "CASH", "CREDIT_CARD"})
    private String paymentMethod;
}