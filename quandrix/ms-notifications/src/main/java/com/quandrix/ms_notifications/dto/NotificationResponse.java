package com.quandrix.ms_notifications.dto;

import java.time.LocalDateTime;
import com.quandrix.ms_notifications.model.NotificationType;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Schema(title = "Notification Response", description = "Respuesta del contenido de una notificación")
public class NotificationResponse {

    @Schema(description = "Id de la notificación", example = "1")
    private Long id;

    @Schema(description = "Id del usuario", example = "1")
    private Long userId;

    @Schema(description = "Tipo de notificación", example = "NEW_ORDER", allowableValues = {"PAYMENT_CONFIRMED", "LISTING_SOLD",
     "REVIEW_RECEIVED", "ORDER_CANCELLED"})
    private NotificationType type;

    @Schema(description = "Contenido de la notificación", example = "LISTING_SOLD: Su publicación ha sido vendida")
    private String message;

    @Schema(description = "Notificación leida/no leída", example = "false")
    private boolean readFlag;

    @Schema(description = "Fecha de creación de la notificación", example = "2026-06-12T14:30:00")
    private LocalDateTime createdAt;
}
