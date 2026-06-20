package com.quandrix.ms_reviews.dto;

import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Schema(title = "Respuesta de una reseña", description = "Respuesta con los datos de una reseña")
public class ReviewResponse {

    @Schema(description = "Id de notificación", example = "1")
    private Long id;

    @Schema(description = "Id de la reseña", example = "1")
    private Long reviewerId;

    @Schema(description = "Id del vendedor", example = "1")
    private Long sellerId;

    @Schema(description = "Calificación", example = "1-5")
    private Integer rating;

    @Schema(description = "Cuerpo de la reseña", example = "Buenas cartas")
    private String comment;

    @Schema(description = "Fecha de creación de la reseña", example = "2026-06-12T14:30:00")
    private LocalDateTime createdAt;
}
