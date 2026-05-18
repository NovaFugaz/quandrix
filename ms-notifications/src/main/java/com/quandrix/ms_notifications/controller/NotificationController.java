package com.quandrix.ms_notifications.controller;

import com.quandrix.ms_notifications.dto.NotificationRequest;
import com.quandrix.ms_notifications.dto.NotificationResponse;
import com.quandrix.ms_notifications.service.NotificationService;

import jakarta.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/notifications")
public class NotificationController {

    private static final Logger log =
            LoggerFactory.getLogger(NotificationController.class);

    private final NotificationService notificationService;

    public NotificationController(
            NotificationService notificationService
    ) {
        this.notificationService = notificationService;
    }

    /**
     * Crear una notificación
     */
    @PostMapping
    public ResponseEntity<NotificationResponse> create(
            @Valid @RequestBody NotificationRequest request
    ) {

        log.info(
                "POST /notifications userId={}",
                request.getUserId()
        );

        NotificationResponse response =
                notificationService.create(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    /**
     * Obtener todas las notificaciones de un usuario
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<NotificationResponse>> getByUser(
            @PathVariable Long userId
    ) {

        log.info(
                "GET /notifications/user/{}",
                userId
        );

        return ResponseEntity.ok(
                notificationService.getByUser(userId)
        );
    }

    /**
     * Obtener notificaciones no leídas
     */
    @GetMapping("/user/{userId}/unread")
    public ResponseEntity<List<NotificationResponse>> getUnread(
            @PathVariable Long userId
    ) {

        log.info(
                "GET /notifications/user/{}/unread",
                userId
        );

        return ResponseEntity.ok(
                notificationService.getUnreadByUser(userId)
        );
    }

    /**
     * Contar notificaciones no leídas
     */
    @GetMapping("/user/{userId}/unread/count")
    public ResponseEntity<Map<String, Long>> countUnread(
            @PathVariable Long userId
    ) {

        log.info(
                "GET /notifications/user/{}/unread/count",
                userId
        );

        long count =
                notificationService.countUnread(userId);

        return ResponseEntity.ok(
                Map.of("unread", count)
        );
    }

    /**
     * Marcar una notificación como leída
     */
    @PatchMapping("/{id}/read")
    public ResponseEntity<NotificationResponse> markAsRead(
            @PathVariable Long id
    ) {

        log.info(
                "PATCH /notifications/{}/read",
                id
        );

        return ResponseEntity.ok(
                notificationService.markAsRead(id)
        );
    }

    /**
     * Marcar todas las notificaciones de un usuario como leídas
     */
    @PatchMapping("/user/{userId}/read-all")
    public ResponseEntity<Void> markAllAsRead(
            @PathVariable Long userId
    ) {

        log.info(
                "PATCH /notifications/user/{}/read-all",
                userId
        );

        notificationService.markAllAsRead(userId);

        return ResponseEntity.noContent().build();
    }

    /**
     * Eliminar una notificación
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable Long id
    ) {

        log.info(
                "DELETE /notifications/{}",
                id
        );

        notificationService.delete(id);

        return ResponseEntity.noContent().build();
    }
}