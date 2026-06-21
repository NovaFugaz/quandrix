package com.quandrix.ms_reviews.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Schema(title = "Respuesta de calificación", description = "Respuesta con los datos de una calificación a un vendedor")
public class SellerRatingResponse {

    @Schema(description = "Id del vendedor", example = "1")
    private Long sellerId;

    @Schema(description = "Promedio de la calificación", example = "4.5")
    private Double averageRating;

    @Schema(description = "Cantidad total de reseñas", example = "20")
    private Long totalReviews;
}
