package com.quandrix.ms_listings.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(title = "Card Response", description = "Respuesta del contenido de una carta")
public class CardResponse {

    @Schema(description = "Id único de la carta", example = "bd8fa8c8-7e1c-4f5d-a6d3-123456789abc")
    private String scryfallId;

    @Schema(description = "Nombre de la carta", example = "Black Lotus")
    private String name;

    @Schema(description = "Rareza de la carta", example = "RARE")
    private String rarity;
}