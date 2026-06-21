package com.quandrix.ms_reviews.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "Datos necesarios para la reseña")
public class ReviewRequest {

    @NotNull(message = "El reviewerId es obligatorio")
    @Schema(description = "Id de la reseña", example = "1")
    private Long reviewerId;

    @NotNull(message = "El sellerId es obligatorio")
    @Schema(description = "Id del vendor", example = "1")
    private Long sellerId;

    @NotNull(message = "El rating es obligatorio")
    @Min(value = 1, message = "El raiting su mínimo es 1")
    @Max(value = 5, message = "El raiting su máximo es 5")
    @Schema(description = "Calificación", example = "1-5")
    private Integer rating;

    @Size(max = 500, message = "El comentario no puede superar los 500 caracteres")
    @Schema(description = "Cuerpo de la reseña", example = "Buenas cartas")
    private String comment;
}
