package com.quandrix.ms_reviews.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Schema(description = "Datos necesarios para una notificación")
public class NotificationRequest {

    @Schema(description = "Id de usuario", example = "1")
    private Long userId;

    @Schema(description = "Tipo de notificación", example = "NEW_ORDER")
    private String type;

    @Schema(description = "Cuerpo de la notificación", example = "Publicación vendida")
    private String message;
}
