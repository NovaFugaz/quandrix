package com.quandrix.ms_listings.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "Objeto utilizado para crear/actualizar una publicación")
public class ListingRequest {

    @NotNull(message = "El sellerId es obligatorio")
    @Schema(description = "Id vendedor", example = "1")
    private Long sellerId;

    // El usuario proporciona nombre y set en lugar de scryfallId
    @NotBlank(message = "El nombre de la carta es obligatorio")
    @Schema(description = "Nombre de carta", example = "Black Lotus")
    private String cardName;

    // Opcional — si no se provee toma la primera coincidencia
    private String setCode;

    @NotNull(message = "La condición es obligatoria")
    @Schema(description = "Conservación carta", example = "MINT")
    private String condition;

    @Min(value = 100, message = "El precio mínimo es $100")
    @Positive(message = "El precio debe ser positivo")
    @Schema(description = "Precio", example = "5000")
    private Long price;

    @NotNull(message = "La cantidad es obligatoria")
    @Min(value = 1, message = "La cantidad mínima es 1")
    @Max(value = 100, message = "La cantidad máxima es 100")
    @Positive(message = "La cantidad debe ser positiva")
    @Schema(description = "Cantidad", example = "3")
    private Integer quantity;
}