package com.quandrix.ms_payments.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(title = "Payment Request", description = "Datos necesarios para el pago")
public class PaymentRequest {

    @NotNull(message = "El orderId es obligatorio")
    @Schema(description = "Id de la orden", example = "1")
    private Long orderId;

    @NotNull(message = "El monto es obligatorio")
    @Min(value = 1, message = "El monto mínimo es 1")
    @Schema(description = "Monto a pagar", example = "5000")
    private Long amount;

    @NotBlank(message = "El método de pago es obligatorio")
    @Schema(description = "Metodo para pagar", example = "CREDIT_CARD", allowableValues = {"DEBIT_CARD", "BANK_TRANSFER", "CASH", "CREDIT_CARD"})
    private String method;

// Opción para los tests de defensa, permite forzar un fallo de pago sin depender de la aleatoriedad.
// No se expone en la API pública, solo para pruebas internas.
// Si se establece en true, el pago se rechazará de forma determinística, útil para probar flujos de error.  
    @Builder.Default
    private Boolean forceFailure = false;
}