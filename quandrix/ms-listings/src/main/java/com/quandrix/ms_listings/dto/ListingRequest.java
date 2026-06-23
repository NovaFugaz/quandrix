package com.quandrix.ms_listings.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(title = "Listings Request", description = "Datos necesarios ara crear/actualizar una publicación")
public class ListingRequest {

    @NotNull(message = "El sellerId es obligatorio")
    @Schema(description = "Id vendedor", example = "1")
    private Long sellerId;

    @NotBlank(message = "El nombre de la carta es obligatorio")
    @Schema(description = "Nombre de carta", example = "Black Lotus")
    private String cardName;

    @Schema(description = "Código del set de la carta", example = "LEA")
    private String setCode;

    @NotNull(message = "La condición es obligatoria")
    @Schema(description = "Conservación carta", example = "MINT", allowableValues = {"MINT", "NEAR_MINT", "EXCELLENT", "GOOD", 
    "LIGHT_PLAYED", "HEAVILY_PLAYED", "POOR", "DAMAGED"})
    private String condition;

    @Min(value = 100, message = "El precio mínimo es $100")
    @Positive(message = "El precio debe ser positivo")
    @Schema(description = "Precio unitario de la carta", example = "5000")
    private Long price;

    @NotNull(message = "La cantidad es obligatoria")
    @Min(value = 1, message = "La cantidad mínima es 1")
    @Max(value = 100, message = "La cantidad máxima es 100")
    @Positive(message = "La cantidad debe ser positiva")
    @Schema(description = "Cantidad", example = "3")
    private Integer quantity;
}