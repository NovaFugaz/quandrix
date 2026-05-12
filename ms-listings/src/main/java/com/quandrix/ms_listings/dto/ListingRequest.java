package com.quandrix.ms_listings.dto;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;

@Getter
@Setter
public class ListingRequest {

    @NotNull(message = "El sellerId es obligatorio")
    private Long sellerId;

    @NotBlank(message = "El scryfallId es obligatorio")
    private String scryfallId;

    @NotNull(message = "La condición es obligatoria")
    private String condition;

    @NotNull(message = "El precio es obligatorio")
    @DecimalMin(value = "0.01", message = "El precio debe ser mayor a 0")
    private BigDecimal price;

    @NotNull(message = "La cantidad es obligatoria")
    @Min(value = 1, message = "La cantidad mínima es 1")
    private Integer quantity;
}