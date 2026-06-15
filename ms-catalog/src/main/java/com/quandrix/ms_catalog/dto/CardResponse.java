package com.quandrix.ms_catalog.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Schema(description = "Información de una carta")
public class CardResponse {
    @Schema(description = "Id único de la carta", example = "bd8fa8c8-7e1c-4f5d-a6d3-123456789abc")
    private String scryfallId;

    @Schema(description = "Nombre de la carta", example = "Black Lotus")
    private String name;

    @Schema(description = "Código del set de la carta", example = "LEA")
    private String setCode;

    @Schema(description = "Nombre del set de la carta", example = "Limited Edition Alpha")
    private String setName;

    @Schema(description = "URL de la imagen de la carta", example = "https://cards.scryfall.io/normal/front/example.jpg")
    private String imageUrl;
}