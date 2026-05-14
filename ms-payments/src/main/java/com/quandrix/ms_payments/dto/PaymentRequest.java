package com.quandrix.ms_payments.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PaymentRequest {

    @NotNull(message = "El orderId es obligatorio")
    private Long orderId;

    @NotNull(message = "El monto es obligatorio")
    @Min(value = 1, message = "El monto mínimo es 1")
    private Long amount;

    @NotBlank(message = "El método de pago es obligatorio")
    private String method;
}