package com.quandrix.ms_notifications.controller;

import com.quandrix.ms_notifications.dto.NotificationRequest;
import com.quandrix.ms_notifications.dto.NotificationResponse;
import com.quandrix.ms_notifications.service.NotificationService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Notificaciones", description = "Operaciones relacionadas a las notificaciones")
public class NotificationController {

        private static final Logger log = LoggerFactory.getLogger(NotificationController.class);

        private final NotificationService notificationService;

        public NotificationController(
                        NotificationService notificationService) {
                this.notificationService = notificationService;
        }

        
        @PostMapping
        @Operation(summary = "Crear una notificación", description = "Crea una nueva notificación para un usuario")
        @ApiResponses(value = {
                @ApiResponse(responseCode = "201", description = "Notificación creada con exito",
                        content = @Content(mediaType = "application/json", schema = @Schema(implementation = NotificationResponse.class))
                ),
                @ApiResponse(responseCode = "400", description = "Datos de la notificación inválidos"),
                @ApiResponse(responseCode = "500", description = "Error interno del servidor")
        })
        public ResponseEntity<NotificationResponse> create(@Valid @RequestBody NotificationRequest request) {
                log.info("POST /notifications userId={}", request.getUserId());
                NotificationResponse response = notificationService.create(request);
                return ResponseEntity.status(HttpStatus.CREATED).body(response);
        }

        
        @GetMapping("/user/{userId}")
        @Operation(summary = "Obtener notificaciones de un usuario", description = "Retorna todas las notificaciones de un usuario.")
        @ApiResponses(value = {
                @ApiResponse(responseCode = "200", description = "Notificaciones encontradas con exito",
                        content = @Content(mediaType = "application/json",
                        array = @ArraySchema(schema = @Schema(implementation = NotificationResponse.class)))
                ),
                @ApiResponse(responseCode = "404", description = "No se encontraron notificaciones de ese usuario"),
                @ApiResponse(responseCode = "500", description = "Error interno del servidor")
        })
        public ResponseEntity<List<NotificationResponse>> getByUser(
                @Parameter(description = "Id del usuario", required = true, example = "1")
                @PathVariable Long userId) {
                log.info("GET /notifications/user/{}",userId);
                return ResponseEntity.ok(notificationService.getByUser(userId));
        }


        @GetMapping("/user/{userId}/unread")
        @Operation(summary = "Obtener notificaciones no leídas", description = "Retorna todas las notificaciones no leídas de un usuario")
        @ApiResponses(value = {
                @ApiResponse(responseCode = "200", description = "Notificación no leídas encontradas con exito",
                        content = @Content(mediaType = "application/json",
                        array = @ArraySchema(schema = @Schema(implementation = NotificationResponse.class)))
                ),
                @ApiResponse(responseCode = "404", description = "No se encontraron notificaciones no leídas"),
                @ApiResponse(responseCode = "500", description = "Error interno del servidor")
        })
        public ResponseEntity<List<NotificationResponse>> getUnread(
                @Parameter(description = "Id del usuario", required = true, example = "1")
                @PathVariable Long userId) {
                log.info("GET /notifications/user/{}/unread",userId);
                return ResponseEntity.ok(notificationService.getUnreadByUser(userId));
        }

        
        @GetMapping("/user/{userId}/unread/count")
        @Operation(summary = "Conteo de notificaciones no leídas", description = "Retorna la cantidad de notificaciones no leídas de un usuario")
        @ApiResponses(value = {
                @ApiResponse(responseCode = "200", description = "Conteo obtenido con exito", 
                content = @Content(mediaType = "application/json", schema = @Schema(example = "{\"unread\":5}"))),
                @ApiResponse(responseCode = "404", description = "No se encontro ese usuario"),
                @ApiResponse(responseCode = "500", description = "Error interno del servidor")
        })
        public ResponseEntity<Map<String, Long>> countUnread(
                @Parameter(description = "Id del usuario", required = true, example = "1")
                @PathVariable Long userId) {
                log.info("GET /notifications/user/{}/unread/count", userId);
                long count = notificationService.countUnread(userId);
                return ResponseEntity.ok(Map.of("unread", count));
        }

        
        @PatchMapping("/{id}/read")
        @Operation(summary = "Marcar una notificación como leída", description = "Marca una notificación especifica como leída")
        @ApiResponses(value = {
                @ApiResponse(responseCode = "200", description = "Notificación marcada como leída con exito",
                        content = @Content(mediaType = "application/json", schema = @Schema(implementation = NotificationResponse.class))
                ),
                @ApiResponse(responseCode = "404", description = "No se encontro la notificación"),
                @ApiResponse(responseCode = "500", description = "Error interno del servidor")
        })
        public ResponseEntity<NotificationResponse> markAsRead(
                @Parameter(description = "Id de la notificación", required = true, example = "1")
                @PathVariable Long id) {
                log.info("PATCH /notifications/{}/read",id);
                return ResponseEntity.ok(notificationService.markAsRead(id));
        }

        
        @PatchMapping("/user/{userId}/read-all")
        @Operation(summary = "Marcar todas las notificaciones como leídas", description = "Marca todas las notificaciones de un usuario como leídas")
        @ApiResponses(value = {
                @ApiResponse(responseCode = "204", description = "Todas las notificaciones han sido marcadas como leídas con exito"),
                @ApiResponse(responseCode = "404", description = "No se encontro el usuario"),
                @ApiResponse(responseCode = "500", description = "Error interno del servidor")
        })
        public ResponseEntity<Void> markAllAsRead(
                @Parameter(description = "Id del usuario", required = true, example = "1")
                @PathVariable Long userId) {
                log.info("PATCH /notifications/user/{}/read-all",userId);
                notificationService.markAllAsRead(userId);
                return ResponseEntity.noContent().build();
        }

        @DeleteMapping("/{id}")
        @Operation(summary = "Eliminar una notificación", description = "Elimina una notificación ingresando su Id")
        @ApiResponses(value = {
                @ApiResponse(responseCode = "204", description = "Notificación eliminada con exito"),
                @ApiResponse(responseCode = "404", description = "No se encontro la notificación"),
                @ApiResponse(responseCode = "500", description = "Error interno del servidor")
        })
        public ResponseEntity<Void> delete(
                @Parameter(description = "Id de la notificación a eliminar", required = true, example = "1")
                @PathVariable Long id) {
                log.info("DELETE /notifications/{}", id);
                notificationService.delete(id);
                return ResponseEntity.noContent().build();
        }
}