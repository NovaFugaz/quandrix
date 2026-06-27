package com.quandrix.ms_notifications.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(title = "Notifications request", description = "Datos necesarios para crear una notificación")
public class NotificationRequest {

    @NotNull(message = "El userId es obligatorio")
    @Schema(description = "Id del usuario", example = "1")
    private Long userId;

    @NotBlank(message = "El tipo es obligatorio")
    @Schema(description = "Tipo de notificación", example = "NEW_ORDER", 
    allowableValues = {"PAYMENT_CONFIRMED", "LISTING_SOLD", "REVIEW_RECEIVED", "ORDER_CANCELLED"})
    private String type;

    @NotBlank(message = "El mensaje es obligatorio")
    @Schema(description = "Contenido de la notificación", example = "LISTING_SOLD: Su publicación ha sido vendida")
    private String message;
}
