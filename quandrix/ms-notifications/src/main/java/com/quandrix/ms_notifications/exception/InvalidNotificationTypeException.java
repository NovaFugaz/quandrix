package com.quandrix.ms_notifications.exception;

public class InvalidNotificationTypeException extends RuntimeException {
    public InvalidNotificationTypeException(String type) {
        super("Tipo de notificación invalido: " + type +
            ". Valores válidos: NUEVA_ORDEN, PAGO_CONFIRMADO, " +
            "PUBLICACION_VENDIDA, RESEÑA_RECIBIDA, ORDEN_CANCELADA"
        );
    }
}

