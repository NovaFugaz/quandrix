package com.quandrix.ms_reviews.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReviewRequest {

    @NotNull(message = "El reviewerId es obligatorio")
    private Long reviewerId;

    @NotNull(message = "El sellerId es obligatorio")
    private Long sellerId;

    @NotNull(message = "El rating es obligatorio")
    @Min(value = 1, message = "El raiting su mínimo es 1")
    @Max(value = 5, message = "El raiting su máximo es 5")
    private Integer rating;

    @Size(max = 500, message = "El comentario no puede superar los 500 caracteres")
    private String comment;
}
