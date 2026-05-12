package com.quandrix.ms_orders.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OrderRequest {

    @NotNull(message = "El buyerId es obligatorio")
    private Long buyerId;

    @NotNull(message = "El listingId es obligatorio")
    private Long listingId;

    @NotBlank(message = "El método de pago es obligatorio")
    private String paymentMethod;
}