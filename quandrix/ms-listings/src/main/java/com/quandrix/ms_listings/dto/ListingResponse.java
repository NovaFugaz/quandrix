package com.quandrix.ms_listings.dto;

import com.quandrix.ms_listings.model.CardCondition;
import com.quandrix.ms_listings.model.ListingStatus;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
@Schema(title = "Listings Response", description = "Respuesta del contenido de una publicación")
public class ListingResponse {

    @Schema(description = "Id publicación", example = "1")
    private Long id;

    @Schema(description = "Id vendedor", example = "1")
    private Long sellerId;

    @Schema(description = "Id scryfallId", example = "bd8fa8c8-7e1c-4f5d-a6d3-123456789abc")
    private String scryfallId;

    @Schema(description = "Conservación carta", example = "MINT", allowableValues = {"MINT", "NEAR_MINT", "EXCELLENT", "GOOD", 
    "LIGHT_PLAYED", "HEAVILY_PLAYED", "POOR", "DAMAGED"})
    private CardCondition condition;

    @Schema(description = "Precio", example = "5000")
    private Long price;

    @Schema(description = "Cantidad", example = "3")
    private Integer quantity;

    @Schema(description = "Estado publicación", example = "ACTIVE", allowableValues = {"ACTIVE", "INACTIVE", "WITHDRAWN", "SOLD"})
    private ListingStatus status;

    @Schema(description = "Fecha creación publicación", example = "2026-06-12T14:30:00")
    private LocalDateTime createdAt;
}