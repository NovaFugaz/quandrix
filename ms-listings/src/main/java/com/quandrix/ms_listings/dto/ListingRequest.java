package com.quandrix.ms_listings.dto;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ListingRequest {

    @NotNull(message = "El sellerId es obligatorio")
    private Long sellerId;

    // El usuario proporciona nombre y set en lugar de scryfallId
    @NotBlank(message = "El nombre de la carta es obligatorio")
    private String cardName;

    // Opcional — si no se provee toma la primera coincidencia
    private String setCode;

    @NotNull(message = "La condición es obligatoria")
    private String condition;

    @Min(value = 1, message = "El precio mínimo es $1")
    private Long price;

    @NotNull(message = "La cantidad es obligatoria")
    @Min(value = 1, message = "La cantidad mínima es 1")
    private Integer quantity;
}